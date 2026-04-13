package com.example.manager.util;

import java.util.Random;

public class TeamNameGenerator {

    private static final String[] PREFIXES = {
        "SV", "FC", "SSV", "FSV", "VfB", "TSV", "1. FC", "2. FC",
        "Sporting", "Athletic", "Real", "CF", "CD", "AS", "AS Roma",
        "United", "City", "Rangers", "Celtic", "Ajax", "PSV"
    };

    private static final String[] CITY_NAMES = {
        "Berlin", "Munich", "Frankfurt", "Cologne", "Hamburg", "Düsseldorf",
        "Stuttgart", "Dresden", "Leipzig", "Hanover", "Mainz", "Augsburg",
        "Wolfsburg", "Leverkusen", "Hoffenheim", "Freiburg", "Schalke",
        "Dortmund", "Bremen", "Rostock", "Nuremberg", "Kaiserslautern",
        "Bielefeld", "Bochum", "Wuppertal", "Gelsenkirchen", "Oberhausen",
        "Aachen", "Münster", "Paderborn", "Bayreuth", "Würzburg"
    };

    private static final String[] SUFFIXES = {
        "", " United", " City", " Athletic", " Rangers", " Celtic",
        " United", " Atletico", " CF", " Club", " Association"
    };

    private static final Random RANDOM = new Random();

    public static String generateTeamName() {
        String prefix = PREFIXES[RANDOM.nextInt(PREFIXES.length)];
        String city = CITY_NAMES[RANDOM.nextInt(CITY_NAMES.length)];
        String suffix = SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
        
        // Vermeide doppelte Präfixe
        if ((prefix.contains("FC") || prefix.contains("CF")) && suffix.contains("CF")) {
            suffix = "";
        }
        
        return prefix + " " + city + suffix;
    }
}
