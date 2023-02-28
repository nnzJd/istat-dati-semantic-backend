package it.gov.innovazione.ndc.controller.exception;

public class VocabularyDataNotFoundException extends BaseNotFoundException {
    public VocabularyDataNotFoundException(String index) {
        super("Unable to find vocabulary data for : " + index);
    }
}
