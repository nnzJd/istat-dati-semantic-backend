package it.gov.innovazione.ndc.harvester.csv;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
public class CsvParser {
    private final List<HeadersToIdNameExtractor> nameExtractors;

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class CsvData {
        private final List<Map<String, String>> records;
        private final String idName;
    }


    public CsvData loadCsvDataFromFile(String csvFile) {
        try (FileReader csvReader = new FileReader(csvFile, UTF_8)) {
            return tryParseCsv(csvReader, csvFile);

        } catch (IOException e) {
            throw new InvalidCsvException(String.format("Cannot parse CSV file '%s'", csvFile), e);
        }
    }

    private CsvData tryParseCsv(FileReader csvReader, String csvFile) throws IOException {
        try (CSVParser parser = parseReader(csvReader)) {
            return buildCsvDataFromParser(parser, csvFile);
        }
    }

    private CsvData buildCsvDataFromParser(CSVParser parser, String csvFile) {
        String idName = getIdName(parser, csvFile);
        List<Map<String, String>> records = readRecords(parser);

        return new CsvData(records, idName);
    }

    private CSVParser parseReader(FileReader csvReader) throws IOException {
        return Objects.requireNonNull(CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(csvReader));
    }

    private List<Map<String, String>> readRecords(CSVParser parser) {
        return parser.stream()
                .map(CSVRecord::toMap)
                .collect(Collectors.toList());
    }

    private String getIdName(CSVParser parser, String csvFile) {
        List<String> headerNames = parser.getHeaderNames();
        if (headerNames.isEmpty()) {
            throw new InvalidCsvException(String.format("Cannot find any headers in '%s'", csvFile));
        }

        return nameExtractors.stream()
                .map(e -> e.extract(headerNames))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new InvalidCsvException(String.format("Cannot find id column in '%s'", csvFile)));
    }
}