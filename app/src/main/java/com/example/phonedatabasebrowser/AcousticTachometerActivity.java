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

        spectrumView = new SpectrumView();
        root.addView(spectrumView, new LinearLayout.LayoutParams(-1, 260));

        root.addView(text("Графиката показва амплитудата спрямо честотата. Пикът маркира откритата честота.", 12, 0xFF475569));

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
        short[] samples = new short[FFT_SIZE];
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
        double[] values = new double[FFT_SIZE];
        double mean = 0;
        for (short sample : pcm) mean += sample;
        mean /= pcm.length;

        double rms = 0;
        for (int i = 0; i < pcm.length; i++) {
            double window = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (pcm.length - 1));
            values[i] = (pcm[i] - mean) * window;
            rms += values[i] * values[i];
        }
        rms = Math.sqrt(rms / pcm.length);
        if (rms < 12) {
            runOnUiThread(() -> statusText.setText("Сигналът е слаб. Приближи микрофона или намали околния шум."));
            return;
        }

        double[] magnitudes = fftMagnitudes(values);
        spectrum = magnitudes;
        double fftHz = findDominantFrequency(magnitudes);
        double clickHz = estimateClickRate(pcm);
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

        runOnUiThread(() -> {
            rpmText.setText(rpm > 0 ? String.format(Locale.US, "%.0f RPM", rpm) : "--- RPM");
            frequencyText.setText(String.format(Locale.US, "Открита честота: %.2f Hz  |  FFT пик: %.2f Hz", chosenHz, fftHz));
            statusText.setText(String.format(Locale.US, "Ниво на сигнала: %.0f  |  Импулси/оборот: %d", rms, pulsesPerRevolution));
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

    // Търси най-силния спектрален пик в полезния диапазон 5–5000 Hz.
    private double findDominantFrequency(double[] magnitude) {
        int first = Math.max(1, (int) (5.0 * FFT_SIZE / SAMPLE_RATE));
        int last = Math.min(magnitude.length - 2, (int) (5000.0 * FFT_SIZE / SAMPLE_RATE));
        int best = first;
        for (int i = first + 1; i <= last; i++) if (magnitude[i] > magnitude[best]) best = i;
        if (magnitude[best] <= 0) return 0;
        // Параболична интерполация подобрява оценката между два FFT бин-а.
        double left = Math.log(magnitude[best - 1] + 1e-12);
        double center = Math.log(magnitude[best] + 1e-12);
        double right = Math.log(magnitude[best + 1] + 1e-12);
        double denom = left - 2 * center + right;
        double correction = denom == 0 ? 0 : 0.5 * (left - right) / denom;
        return (best + correction) * SAMPLE_RATE / FFT_SIZE;
    }

    // Открива повтарящите се удари по пиковете на звуковата обвивка.
    private double estimateClickRate(short[] pcm) {
        int envelopeSize = 512;
        double[] envelope = new double[envelopeSize];
        int block = pcm.length / envelopeSize;
        for (int i = 0; i < envelopeSize; i++) {
            double sum = 0;
            for (int k = 0; k < block; k++) sum += Math.abs(pcm[i * block + k]);
            envelope[i] = sum / block;
        }
        double avg = 0;
        for (double value : envelope) avg += value;
        avg /= envelope.length;
        for (int i = 0; i < envelope.length; i++) envelope[i] -= avg;

        int minLag = Math.max(1, (int) (SAMPLE_RATE / (block * 80.0)));
        int maxLag = Math.min(envelope.length / 2, (int) (SAMPLE_RATE / (block * 0.5)));
        double best = 0;
        int bestLag = 0;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double correlation = 0;
            for (int i = 0; i < envelope.length - lag; i++) correlation += envelope[i] * envelope[i + lag];
            if (correlation > best) { best = correlation; bestLag = lag; }
        }
        if (bestLag == 0) return 0;
        double confidence = best / (envelope.length * (avg * avg + 1));
        if (confidence < 1) return 0;
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
            paint.setColor(0xFF64748B); paint.setTextSize(25);
            canvas.drawText("FFT спектър (Hz)", 14, 28, paint);
            double[] data = spectrum;
            double max = 1;
            for (int i = 1; i < data.length; i++) if (data[i] > max) max = data[i];
            paint.setColor(0xFF0878D1); paint.setStrokeWidth(2);
            float prevX = 0, prevY = h;
            for (int x = 1; x < (int) w; x++) {
                int bin = Math.max(1, Math.min(data.length - 1, (int) (x * (5000.0 * FFT_SIZE / SAMPLE_RATE) / w)));
                float y = h - 12 - (float) (Math.log1p(data[bin]) / Math.log1p(max) * (h - 55));
                if (x > 1) canvas.drawLine(prevX, prevY, x, y, paint);
                prevX = x; prevY = y;
            }
            float peakX = (float) (peakFrequency / 5000.0 * w);
            paint.setColor(0xFFE04B59); paint.setStrokeWidth(3);
            canvas.drawLine(peakX, 35, peakX, h - 12, paint);
            paint.setTextSize(22);
            canvas.drawText(String.format(Locale.US, "%.1f Hz", peakFrequency), Math.max(8, Math.min(w - 100, peakX + 5)), 52, paint);
            paint.setColor(0xFF475569); paint.setTextSize(20);
            canvas.drawText("0", 8, h - 5, paint);
            canvas.drawText("5000 Hz", Math.max(8, w - 90), h - 5, paint);
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
