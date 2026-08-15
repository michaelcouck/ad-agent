package com.ikube.analytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ExcessMortality {

    private static final String VACCINATIONS_RESOURCE = "data/country_vaccinations.csv";
    private static final String MORTALITY_RESOURCE = "data/world_mortality.csv";
    private static final int BASELINE_START_YEAR = 2017;
    private static final int BASELINE_END_YEAR = 2019;
    private static final int EXCESS_START_YEAR = 2022;
    private static final int EXCESS_END_YEAR = 2024;
    private static final int REQUIRED_MORTALITY_START_YEAR = 2022;
    private static final int REQUIRED_MORTALITY_END_YEAR = 2024;

    public static void main(String[] args) throws IOException {
        List<ExcessMortalityResult> results = calculate();

        System.out.println("country,people_vaccinated_per_hundred,year,deaths,baseline_average_deaths,excess_deaths,excess_deaths_percent");
        for (ExcessMortalityResult result : results) {
            System.out.printf(
                    "%s,%.2f,%d,%.0f,%.2f,%.2f,%.2f%n",
                    result.country(),
                    result.peopleVaccinatedPerHundred(),
                    result.year(),
                    result.deaths(),
                    result.baselineAverageDeaths(),
                    result.excessDeaths(),
                    result.excessDeathsPercent()
            );
        }

        Optional<Double> pearsonCorrelation = pearsonCorrelation(results);
        System.out.println();
        System.out.println("pearson_correlation_people_vaccinated_per_hundred_to_excess_deaths_percent");
        if (pearsonCorrelation.isPresent()) {
            System.out.printf("%.6f%n", pearsonCorrelation.get());
        } else {
            System.out.println("undefined");
        }
    }

    public static List<ExcessMortalityResult> calculate() throws IOException {
        Map<String, Double> collectionA = loadGreatestVaccinatedPerHundred();
        Map<String, Map<Integer, Double>> collectionB = loadDeathsByCountryAndYear(EXCESS_START_YEAR, EXCESS_END_YEAR);
        Map<String, Double> collectionC = loadBaselineAverageDeaths();

        Set<String> intersection = new HashSet<>(collectionA.keySet());
        intersection.retainAll(collectionB.entrySet().stream()
                .filter(entry -> hasMortalityDataForEveryRequiredYear(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));
        intersection.retainAll(collectionC.keySet());

        return intersection.stream()
                .sorted()
                .flatMap(country -> collectionB.get(country).entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            int year = entry.getKey();
                            double deaths = entry.getValue();
                            double baselineAverageDeaths = collectionC.get(country);
                            double excessDeaths = deaths - baselineAverageDeaths;
                            double excessDeathsPercent = baselineAverageDeaths == 0.0
                                    ? 0.0
                                    : (excessDeaths / baselineAverageDeaths) * 100.0;
                            return new ExcessMortalityResult(
                                    country,
                                    collectionA.get(country),
                                    year,
                                    deaths,
                                    baselineAverageDeaths,
                                    excessDeaths,
                                    excessDeathsPercent
                            );
                        }))
                .toList();
    }

    public static Optional<Double> pearsonCorrelation(List<ExcessMortalityResult> results) {
        if (results.size() < 2) {
            return Optional.empty();
        }

        double averageVaccinated = results.stream()
                .mapToDouble(ExcessMortalityResult::peopleVaccinatedPerHundred)
                .average()
                .orElseThrow();
        double averageExcessDeathsPercent = results.stream()
                .mapToDouble(ExcessMortalityResult::excessDeathsPercent)
                .average()
                .orElseThrow();

        double covariance = 0.0;
        double vaccinatedVariance = 0.0;
        double excessDeathsPercentVariance = 0.0;
        for (ExcessMortalityResult result : results) {
            double vaccinatedDelta = result.peopleVaccinatedPerHundred() - averageVaccinated;
            double excessDeathsPercentDelta = result.excessDeathsPercent() - averageExcessDeathsPercent;
            covariance += vaccinatedDelta * excessDeathsPercentDelta;
            vaccinatedVariance += vaccinatedDelta * vaccinatedDelta;
            excessDeathsPercentVariance += excessDeathsPercentDelta * excessDeathsPercentDelta;
        }

        double denominator = Math.sqrt(vaccinatedVariance * excessDeathsPercentVariance);
        if (denominator == 0.0) {
            return Optional.empty();
        }
        return Optional.of(covariance / denominator);
    }

    private static boolean hasMortalityDataForEveryRequiredYear(Map<Integer, Double> deathsByYear) {
        for (int year = REQUIRED_MORTALITY_START_YEAR; year <= REQUIRED_MORTALITY_END_YEAR; year++) {
            if (!deathsByYear.containsKey(year)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Double> loadGreatestVaccinatedPerHundred() throws IOException {
        Map<String, Double> vaccinatedPerHundredByCountry = new HashMap<>();

        readCsv(VACCINATIONS_RESOURCE, row -> {
            String country = row.get("country");
            Optional<Double> peopleVaccinatedPerHundred = parseDouble(row.get("people_vaccinated_per_hundred"));
            if (isBlank(country) || peopleVaccinatedPerHundred.isEmpty()) {
                return;
            }

            vaccinatedPerHundredByCountry.merge(country, peopleVaccinatedPerHundred.get(), Math::max);
        });

        return vaccinatedPerHundredByCountry;
    }

    private static Map<String, Map<Integer, Double>> loadDeathsByCountryAndYear(int startYear, int endYear) throws IOException {
        Map<String, Map<Integer, Double>> deathsByCountryAndYear = new TreeMap<>();

        readCsv(MORTALITY_RESOURCE, row -> {
            String country = row.get("country_name");
            Optional<Integer> year = parseInteger(row.get("year"));
            Optional<Double> deaths = parseDouble(row.get("deaths"));
            if (isBlank(country) || year.isEmpty() || deaths.isEmpty()) {
                return;
            }
            if (year.get() < startYear || year.get() > endYear) {
                return;
            }

            deathsByCountryAndYear
                    .computeIfAbsent(country, ignored -> new TreeMap<>())
                    .merge(year.get(), deaths.get(), Double::sum);
        });

        return deathsByCountryAndYear;
    }

    private static Map<String, Double> loadBaselineAverageDeaths() throws IOException {
        Map<String, Map<Integer, Double>> baselineDeathsByCountryAndYear =
                loadDeathsByCountryAndYear(BASELINE_START_YEAR, BASELINE_END_YEAR);

        return baselineDeathsByCountryAndYear.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElseThrow(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private static void readCsv(String resourceName, CsvRowConsumer rowConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openResource(resourceName), StandardCharsets.UTF_8))) {
            List<String> headers = parseCsvLine(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = parseCsvLine(line);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), i < values.size() ? values.get(i) : "");
                }
                rowConsumer.accept(row);
            }
        }
    }

    private static InputStream openResource(String resourceName) throws IOException {
        InputStream inputStream = ExcessMortality.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream != null) {
            return inputStream;
        }

        Path sourceResourcePath = Path.of("src", "main", "resources", resourceName);
        if (Files.exists(sourceResourcePath)) {
            return Files.newInputStream(sourceResourcePath);
        }

        throw new IOException("Resource not found: " + resourceName);
    }

    private static List<String> parseCsvLine(String line) {
        if (line == null) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        values.add(value.toString());
        return values;
    }

    private static Optional<Integer> parseInteger(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value.trim()));
    }

    private static Optional<Double> parseDouble(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(value.trim()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface CsvRowConsumer {
        void accept(Map<String, String> row) throws IOException;
    }

    public record ExcessMortalityResult(
            String country,
            double peopleVaccinatedPerHundred,
            int year,
            double deaths,
            double baselineAverageDeaths,
            double excessDeaths,
            double excessDeathsPercent
    ) {
        public ExcessMortalityResult {
            Objects.requireNonNull(country, "country must not be null");
        }
    }

}
