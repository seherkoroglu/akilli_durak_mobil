package com.example.akillidurak;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.translation.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.Map;

public class SpeechTranslation {
    // This example requires environment variables named "SPEECH_KEY" and "SPEECH_REGION"
    private static String speechKey = System.getenv("SPEECH_KEY");
    private static String speechRegion = System.getenv("SPEECH_REGION");

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SpeechTranslationConfig speechTranslationConfig = SpeechTranslationConfig.fromSubscription(speechKey, speechRegion);
        speechTranslationConfig.setSpeechRecognitionLanguage("en-US");

        String[] toLanguages = { "it" };
        for (String language : toLanguages) {
            speechTranslationConfig.addTargetLanguage(language);
        }

        recognizeFromMicrophone(speechTranslationConfig);
    }

    public static void recognizeFromMicrophone(SpeechTranslationConfig speechTranslationConfig) throws InterruptedException, ExecutionException {
        AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
        TranslationRecognizer translationRecognizer = new TranslationRecognizer(speechTranslationConfig, audioConfig);

        System.out.println("Speak into your microphone.");
        Future<TranslationRecognitionResult> task = translationRecognizer.recognizeOnceAsync();
        TranslationRecognitionResult translationRecognitionResult = task.get();

        if (translationRecognitionResult.getReason() == ResultReason.TranslatedSpeech) {
            System.out.println("RECOGNIZED: Text=" + translationRecognitionResult.getText());
            for (Map.Entry<String, String> pair : translationRecognitionResult.getTranslations().entrySet()) {
                System.out.printf("Translated into '%s': %s\n", pair.getKey(), pair.getValue());
            }
        }
        else if (translationRecognitionResult.getReason() == ResultReason.NoMatch) {
            System.out.println("NOMATCH: Speech could not be recognized.");
        }
        else if (translationRecognitionResult.getReason() == ResultReason.Canceled) {
            CancellationDetails cancellation = CancellationDetails.fromResult(translationRecognitionResult);
            System.out.println("CANCELED: Reason=" + cancellation.getReason());

            if (cancellation.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                System.out.println("CANCELED: Did you set the speech resource key and region values?");
            }
        }

        System.exit(0);
    }
}