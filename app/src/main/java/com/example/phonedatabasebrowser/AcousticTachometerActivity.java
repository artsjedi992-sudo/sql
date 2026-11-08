package com.example.phonedatabasebrowser;

// Android UI, аудио вход и разрешения за микрофона.
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.provider.Settings;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

// Акустичният тахометър измерва периодичността на звука чрез микрофона.
public class AcousticTachometerActivity extends Activity {
    private static final int AUDIO_PERMISSION = 410;
    private static final int SAMPLE_RATE = 44100;
    private static final int FFT_SIZE = 8192;

    // Режимите използват различни методи за намиране на периода на въртене.
    private static final int MODE_CLICKS = 0;
    private static final int MODE_SPECTRUM = 1;
    private static final int MODE_AUTO = 2;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile int mode = MODE_CLICKS;
    private volatile int pulsesPerRevolution = 1;
    private volatile double minFrequencyHz = 1.0;
    private volatile double maxFrequencyHz = 40.0;
    private volatile double envelopeSampleRate = 256.0;
    private volatile double currentRpm = 0;
    private volatile double peakFrequency = 0;
    private volatile double[] spectrum = new double[FFT_SIZE / 2];
    private volatile double[] waveform = new double[512];

    private AudioRecord recorder;
    private Thread audioThread;
    private TextView rpmText;
    private TextView frequencyText;
    private TextView statusText;
    private TextView modeInfo;
    private SpectrumView spectrumView;
    private Spinner modeSpinner;
    private SeekBar pulseSeek;
    private SeekBar minFrequencySeek;
    private SeekBar maxFrequencySeek;
    private TextView frequencyRangeText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        // Android изисква изрично разрешение преди достъп до микрофона.
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudio();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
        }
    }

    // Изгражда екран с избор на алгоритъм, настройка на импулсите и спектрална графика.
    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 12, 18, 18);
        root.setBackgroundColor(0xFFF1F6FC);
        scroll.addView(root);

        TextView title = text("АКУСТИЧЕН ТАХОМЕТЪР", 21, 0xFF103D73);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        rpmText = text("--- RPM", 42, 0xFF087B50);
        rpmText.setGravity(Gravity.CENTER);
        root.addView(rpmText);

        frequencyText = text("Основна честота: --- Hz", 16, Color.DKGRAY);
        frequencyText.setGravity(Gravity.CENTER);
        root.addView(frequencyText);

        root.addView(text("Метод на измерване", 16, 0xFF103D73));
        modeSpinner = new Spinner(this);
        String[] modes = {
                "Повтарящо тракане – импулс на оборот",
                "Спектрален анализ – FFT на звука",
                "Автоматичен анализ – периодичност + FFT"
        };
        modeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                mode = position;
                updateModeInfo();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        root.addView(modeSpinner);

        modeInfo = text("", 13, 0xFF475569);
        root.addView(modeInfo);

        root.addView(text("Брой звукови импулси за един оборот", 15, 0xFF103D73));
        pulseSeek = new SeekBar(this);
        pulseSeek.setMax(19);
        pulseSeek.setProgress(0);
        pulseSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                pulsesPerRevolution = value + 1;
                updateModeInfo();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(pulseSeek);
        root.addView(text("ДОЛНА ГРАНИЦА НА ЧЕСТОТНИЯ ДИАПАЗОН (Hz)", 14, 0xFF103D73));
        minFrequencySeek = new SeekBar(this);
        minFrequencySeek.setMax(255);
        minFrequencySeek.setProgress(2);
        minFrequencySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                minFrequencyHz = Math.max(0.5, value * 0.5);
                if (minFrequencyHz >= maxFrequencyHz) {
                    maxFrequencyHz = Math.min(127.5, minFrequencyHz + 0.5);
                    if (maxFrequencySeek != null) maxFrequencySeek.setProgress((int)Math.round(maxFrequencyHz * 2));
                }
                updateFrequencyRange();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(minFrequencySeek);
        root.addView(text("ГОРНА ГРАНИЦА НА ЧЕСТОТНИЯ ДИАПАЗОН (Hz)", 14, 0xFF103D73));
        maxFrequencySeek = new SeekBar(this);
        maxFrequencySeek.setMax(255);
        maxFrequencySeek.setProgress(80);
        maxFrequencySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                maxFrequencyHz = Math.max(1.0, value * 0.5);
                if (maxFrequencyHz <= minFrequencyHz) {
                    minFrequencyHz = Math.max(0.5, maxFrequencyHz - 0.5);
                    if (minFrequencySeek != null) minFrequencySeek.setProgress((int)Math.round(minFrequencyHz * 2));
                }
                updateFrequencyRange();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(maxFrequencySeek);
        frequencyRangeText = text("Избран диапазон: 1.0 – 40.0 Hz", 14, 0xFF087B50);
        root.addView(frequencyRangeText);

        spectrumView = new SpectrumView();
        root.addView(spectrumView, new LinearLayout.LayoutParams(-1, 260));

        root.addView(text("Фурие хистограма: хоризонтално е честотата на повторение (Hz), вертикално – силата. Зелената зона е избраният диапазон, червената линия е откритият пик.", 12, 0xFF475569));

        statusText = text("Подготвя се микрофонът…", 14, 0xFF103D73);
        root.addView(statusText);

        Button restart = new Button(this);
        restart.setText("Рестартирай измерването");
        restart.setOnClickListener(v -> {
            stopAudio();
            startAudio();
        });
        root.addView(restart);

        Button close = new Button(this);
        close.setText("Затвори");
        close.setOnClickListener(v -> finish());
        root.addView(close);

        setContentView(scroll);
        updateModeInfo();
        updateFrequencyRange();
    }

    private void updateFrequencyRange() {
        if (frequencyRangeText == null) return;
        frequencyRangeText.setText(String.format(Locale.US, "Избран диапазон: %.1f – %.1f Hz", minFrequencyHz, maxFrequencyHz));
        if (spectrumView != null) spectrumView.invalidate();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(8, 9, 8, 9);
        return view;
    }

    // Обяснява какво измерва избраният метод и как се използва настройката за импулси.
    private void updateModeInfo() {
        if (modeInfo == null) return;
        String description;
        if (mode == MODE_CLICKS) {
            description = "Открива разстоянието във времето между повтарящи се тракания. Подходящо е, когато има отчетлив звук веднъж или няколко пъти на оборот.";
        } else if (mode == MODE_SPECTRUM) {
            description = "Изчислява спектъра чрез бързо преобразуване на Фурие (FFT) и търси най-силния периодичен тон. Подходящо за виене и тонални шумове.";
        } else {
            description = "Сравнява периодичността на звуковата обвивка със спектралния пик. Полезно при смесен звук, но резултатът зависи от шума и хармониците.";
        }
        modeInfo.setText(description + "\nНастройка: " + pulsesPerRevolution + " импулс(а) на оборот.");
    }

    // Стартира моно аудио запис с честота 44,1 kHz за анализ на звука.
    private void startAudio() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuffer <= 0) {
            statusText.setText("Телефонът не поддържа избрания аудио режим.");
            return;
        }
        int bufferBytes = Math.max(minBuffer, FFT_SIZE * 2 * 2);
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes);
            // Някои телефони не поддържат UNPROCESSED; преминаваме към стандартния микрофон.
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                recorder.release();
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes);
            }
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                statusText.setText("Микрофонът не може да бъде инициализиран.");
                return;
            }
            recorder.startRecording();
            running.set(true);
            statusText.setText("Измерването работи. Приближи микрофона до източника на звук.");
            audioThread = new Thread(this::audioLoop, "AcousticRpmAnalyzer");
            audioThread.start();
        } catch (SecurityException | IllegalStateException ex) {
            statusText.setText("Няма достъп до микрофона: " + ex.getMessage());
        }
    }

    // Чете непрекъснат аудио поток и подава всеки прозорец към анализа.
    private void audioLoop() {
        // Четирисекунден прозорец позволява измерване и при бавно почукване.
        short[] samples = new short[SAMPLE_RATE * 4];
        while (running.get() && recorder != null) {
            int offset = 0;
            while (offset < samples.length && running.get()) {
                int count = recorder.read(samples, offset, samples.length - offset);
                if (count < 0) break;
                offset += count;
            }
            if (offset == samples.length && running.get()) analyze(samples);
        }
    }

    // Избира подходящия алгоритъм и обновява измерените RPM и спектралната графика.
    private void analyze(short[] pcm) {
        // Нивото се оценява върху целия четирисекунден запис.
        double mean = 0;
        for (short sample : pcm) mean += sample;
        mean /= pcm.length;
        double rms = 0;
        for (short sample : pcm) {
            double centered = sample - mean;
            rms += centered * centered;
        }
        rms = Math.sqrt(rms / pcm.length);
        if (rms < 12) {
            runOnUiThread(() -> statusText.setText("Сигналът е слаб. Приближи микрофона или намали околния шум."));
            return;
        }

        double[] envelope = buildEnvelope(pcm, 1024);
        double envelopeRate = SAMPLE_RATE / (double) (pcm.length / envelope.length);
        envelopeSampleRate = envelopeRate;
        double[] envelopeSpectrum = fftMagnitudes(envelope);
        spectrum = envelopeSpectrum;
        double fftHz = findDominantFrequency(envelopeSpectrum, envelopeRate, minFrequencyHz, maxFrequencyHz);
        double clickHz = estimateClickRate(pcm, minFrequencyHz, maxFrequencyHz);
        double chosenHz;

        if (mode == MODE_CLICKS) chosenHz = clickHz;
        else if (mode == MODE_SPECTRUM) chosenHz = fftHz;
        else chosenHz = clickHz > 0 ? clickHz : fftHz;

        peakFrequency = chosenHz;
        double rpm = chosenHz > 0 ? chosenHz * 60.0 / pulsesPerRevolution : 0;
        currentRpm = rpm;

        // Копира кратка част от сигнала, за да се вижда и времевата форма.
        double[] wave = new double[waveform.length];
        for (int i = 0; i < wave.length; i++) wave[i] = pcm[i * pcm.length / wave.length] / 32768.0;
        waveform = wave;

        // Запазваме моментните стойности във final променливи за безопасно предаване към UI нишката.
        final double rpmUi = rpm;
        final double chosenHzUi = chosenHz;
        final double fftHzUi = fftHz;
        final double rmsUi = rms;
        final int pulsesUi = pulsesPerRevolution;

        runOnUiThread(() -> {
            rpmText.setText(rpmUi > 0 ? String.format(Locale.US, "%.0f RPM", rpmUi) : "--- RPM");
            frequencyText.setText(String.format(Locale.US, "Открита честота: %.2f Hz  |  FFT пик: %.2f Hz", chosenHzUi, fftHzUi));
            statusText.setText(String.format(Locale.US, "Ниво на сигнала: %.0f  |  Импулси/оборот: %d", rmsUi, pulsesUi));
            spectrumView.invalidate();
        });
    }

    // Преобразува сигнала в амплитуден спектър чрез radix-2 FFT.
    private double[] fftMagnitudes(double[] input) {
        int n = input.length;
        double[] real = Arrays.copyOf(input, n);
        double[] imag = new double[n];
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) { j ^= bit; bit >>= 1; }
            j ^= bit;
            if (i < j) {
                double temp = real[i]; real[i] = real[j]; real[j] = temp;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0 * Math.PI / len;
            double wLenR = Math.cos(angle), wLenI = Math.sin(angle);
            for (int start = 0; start < n; start += len) {
                double wr = 1, wi = 0;
                for (int k = 0; k < len / 2; k++) {
                    int even = start + k, odd = even + len / 2;
                    double tr = real[odd] * wr - imag[odd] * wi;
                    double ti = real[odd] * wi + imag[odd] * wr;
                    real[odd] = real[even] - tr; imag[odd] = imag[even] - ti;
                    real[even] += tr; imag[even] += ti;
                    double nextWr = wr * wLenR - wi * wLenI;
                    wi = wr * wLenI + wi * wLenR; wr = nextWr;
                }
            }
        }
        double[] magnitude = new double[n / 2];
        for (int i = 1; i < magnitude.length; i++) magnitude[i] = Math.hypot(real[i], imag[i]);
        return magnitude;
    }

    // Търси най-силния Фурие пик само в зададения от потребителя диапазон.
    private double findDominantFrequency(double[] magnitude, double sampleRate, double minHz, double maxHz) {
        int n = magnitude.length * 2;
        int first = Math.max(1, (int) Math.ceil(minHz * n / sampleRate));
        int last = Math.min(magnitude.length - 2, (int) Math.floor(maxHz * n / sampleRate));
        if (last < first) return 0;
        int best = first;
        for (int i = first + 1; i <= last; i++) if (magnitude[i] > magnitude[best]) best = i;
        if (magnitude[best] <= 0) return 0;
        // Параболична интерполация подобрява оценката между два FFT бин-а.
        double left = Math.log(magnitude[best - 1] + 1e-12);
        double center = Math.log(magnitude[best] + 1e-12);
        double right = Math.log(magnitude[best + 1] + 1e-12);
        double denom = left - 2 * center + right;
        double correction = denom == 0 ? 0 : 0.5 * (left - right) / denom;
        return (best + correction) * sampleRate / n;
    }

    // Открива периода на траканията в четирисекунден запис, включително бавни удари.
    private double[] buildEnvelope(short[] pcm, int envelopeSize) {
        int block = pcm.length / envelopeSize;
        double[] envelope = new double[envelopeSize];
        for (int i = 0; i < envelopeSize; i++) {
            double sum = 0;
            int from = i * block;
            for (int k = 0; k < block; k++) sum += Math.abs(pcm[from + k]);
            envelope[i] = sum / block;
        }
        double avg = 0;
        for (double value : envelope) avg += value;
        avg /= envelope.length;
        for (int i = 0; i < envelope.length; i++) envelope[i] -= avg;
        return envelope;
    }

    private double estimateClickRate(short[] pcm, double minHz, double maxHz) {
        int envelopeSize = 1024;
        int block = pcm.length / envelopeSize;
        double[] envelope = buildEnvelope(pcm, envelopeSize);

        // Премахваме постоянния фон и измерваме енергията на обвивката.
        double energy = 0;
        for (double value : envelope) energy += value * value;
        if (energy < 1.0) return 0;

        double envelopeRate = SAMPLE_RATE / (double) block;
        int minLag = Math.max(2, (int) Math.floor(envelopeRate / maxHz));
        int maxLag = Math.min(envelope.length / 2, (int) Math.ceil(envelopeRate / minHz));
        double bestCorrelation = 0;
        int bestLag = 0;

        // Нормализираната автокорелация не зависи от силата на микрофонния сигнал.
        for (int lag = minLag; lag <= maxLag; lag++) {
            double correlation = 0;
            double energyA = 0;
            double energyB = 0;
            for (int i = 0; i < envelope.length - lag; i++) {
                double a = envelope[i];
                double b = envelope[i + lag];
                correlation += a * b;
                energyA += a * a;
                energyB += b * b;
            }
            double denominator = Math.sqrt(energyA * energyB);
            double normalized = denominator > 0 ? correlation / denominator : 0;
            if (normalized > bestCorrelation) {
                bestCorrelation = normalized;
                bestLag = lag;
            }
        }

        // Под слаб праг не показваме измислени обороти от случаен шум.
        if (bestLag == 0 || bestCorrelation < 0.25) return 0;
        return SAMPLE_RATE / (block * (double) bestLag);
    }

    // Рисува FFT спектъра с честота по хоризонталната ос и амплитуда по вертикалната.
    private class SpectrumView extends View {
        private final Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );
        SpectrumView() { super(AcousticTachometerActivity.this); setBackgroundColor(Color.WHITE); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            paint.setColor(0xFFE2E8F0); paint.setStrokeWidth(1);
            for (int i = 1; i < 5; i++) canvas.drawLine(0, h * i / 5f, w, h * i / 5f, paint);
            double[] data = spectrum;
            double max = 1;
            for (double value : data) if (value > max) max = value;
            float left = 42, right = w - 10, top = 42, bottom = h - 30;
            float pw = Math.max(1, right - left), ph = Math.max(1, bottom - top);
            float x1 = left + (float)(minFrequencyHz / 128.0 * pw);
            float x2 = left + (float)(maxFrequencyHz / 128.0 * pw);
            paint.setColor(0x2230A46C); canvas.drawRect(x1, top, x2, bottom, paint);
            paint.setColor(0xFF64748B); paint.setTextSize(22);
            canvas.drawText("Фурие спектър на повторенията", 12, 27, paint);
            paint.setColor(0xFFE2E8F0); paint.setStrokeWidth(1);
            for (int i = 1; i < 5; i++) canvas.drawLine(left, top + ph * i / 5f, right, top + ph * i / 5f, paint);
            paint.setColor(0xFF0878D1);
            for (int i = 1; i < data.length; i++) {
                float hz = i * (float)envelopeSampleRate / (data.length * 2.0f);
                float x = left + hz / 128.0f * pw;
                float bw = Math.max(1f, pw / data.length + 0.5f);
                float bh = (float)(Math.log1p(data[i]) / Math.log1p(max) * ph);
                canvas.drawRect(x, bottom - bh, x + bw, bottom, paint);
            }
            float peakX = left + (float)(peakFrequency / 128.0 * pw);
            paint.setColor(0xFFE04B59); paint.setStrokeWidth(2);
            canvas.drawLine(peakX, top, peakX, bottom, paint);
            paint.setColor(0xFF475569); paint.setTextSize(18);
            canvas.drawText("0", left, h - 5, paint);
            canvas.drawText("64 Hz", left + pw / 2 - 20, h - 5, paint);
            canvas.drawText("128 Hz", Math.max(left, right - 55), h - 5, paint);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == AUDIO_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startAudio();
        else {
            statusText.setText("Необходимо разрешение за микрофона. Включи го от настройките на приложението.");
            Toast.makeText(this, "Без микрофон акустично измерване не е възможно.", Toast.LENGTH_LONG).show();
        }
    }

    // Спира аудио потока и освобождава микрофона при излизане от екрана.
    private void stopAudio() {
        running.set(false);
        if (recorder != null) {
            try { recorder.stop(); } catch (IllegalStateException ignored) { }
            recorder.release();
            recorder = null;
        }
        if (audioThread != null) {
            try { audioThread.join(300); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            audioThread = null;
        }
    }

    @Override protected void onPause() {
        stopAudio();
        super.onPause();
    }

    @Override protected void onDestroy() {
        stopAudio();
        super.onDestroy();
    }
}
