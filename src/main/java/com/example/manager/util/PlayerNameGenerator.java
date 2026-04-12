package com.example.manager.util;

import java.util.Random;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for generating random player names and countries.
 * Uses predefined lists of names and countries to create unique fictional players.
 */
public class PlayerNameGenerator {
    
    private static final Random random = new Random();
    
    // First names from various cultures
    private static final String[] FIRST_NAMES = {
        // German
        "Stefan", "Klaus", "Ralf", "Jürgen", "Thomas", "Michael", "Franz", "Werner",
        "Hans", "Günther", "Hermann", "Peter", "Wolfgang", "Karl", "Friedrich",
        
        // English
        "John", "James", "David", "Richard", "Edward", "William", "Charles", "Henry",
        "Robert", "Thomas", "George", "Albert", "Arthur", "Frank", "Walter",
        
        // Spanish
        "Carlos", "José", "Manuel", "Juan", "Diego", "Miguel", "Luis", "Antonio",
        "Fernando", "Rafael", "Ricardo", "Pablo", "Javier", "Andrés", "Guillermo",
        
        // Italian
        "Marco", "Giovanni", "Luca", "Andrea", "Alessandro", "Matteo", "Lorenzo",
        "Stefano", "Paolo", "Giuseppe", "Bruno", "Riccardo", "Fabio", "Claudio",
        
        // French
        "Jean", "Pierre", "Michel", "André", "François", "Philippe", "Laurent",
        "Christian", "Marc", "Denis", "Claude", "Xavier", "Serge", "Thierry",
        
        // Dutch
        "Jan", "Pieter", "Willem", "Dirk", "Henk", "Wim", "Bert", "Kees",
        "Ruud", "Erik", "Geert", "Maarten", "Sander", "Robert", "Tom",
        
        // Portuguese
        "João", "Nuno", "Pedro", "Paulo", "André", "Gonçalo", "Marco", "Duarte",
        "Tomás", "Rui", "Tiago", "Bruno", "Daniel", "Vitor", "Filipe",
        
        // Brazilian Portuguese
        "Ronaldo", "Roberto", "Carlos", "Alvaro", "Edson", "Murilo", "Gustavo",
        "Marcelo", "Thiago", "Lucas", "Rodrigo", "Felipe", "Diego", "Rafael",
        
        // Polish
        "Stanisław", "Piotr", "Krzysztof", "Andrzej", "Jerzy", "Tadeusz", "Jan",
        "Józef", "Zbigniew", "Mieczysław", "Ryszard", "Waldemar", "Wacław",
        
        // Czech
        "Jiří", "Josef", "František", "Miroslav", "Václav", "Milan", "Petr",
        "Zdeněk", "Karel", "Jaroslav", "Bohumil", "Lubomír",
        
        // Swedish
        "Anders", "Gustaf", "Erik", "Sven", "Nils", "Ole", "Axel", "Lennart",
        "Hilding", "Folke", "Börje", "Torsten", "Tomas", "Jens",
        
        // Norwegian
        "Ole", "Erik", "Bjørn", "Svein", "Arne", "Leif", "Knut", "Halvard",
        "Roar", "Dag", "Per", "Steinar", "Jarle", "Morten",
        
        // Danish
        "Anders", "Søren", "Henrik", "Jens", "Niels", "Stig", "Kurt", "Morten",
        "Ulrik", "Preben", "Vagn", "Aksel", "Knud",
        
        // Hungarian
        "János", "Péter", "Sándor", "István", "Lajos", "László", "Imre",
        "Gyula", "Tibor", "Ferenc", "József", "Mihály", "Zoltán",
        
        // Serbian
        "Marko", "Slobodan", "Dragan", "Milorad", "Danilo", "Nenad", "Aleksandar",
        "Dejan", "Miroslav", "Vladimir", "Predrag", "Zoran", "Veselin",
        
        // Croatian
        "Marko", "Damir", "Ivo", "Goran", "Darko", "Dragan", "Igor", "Saša",
        "Milan", "Stjepan", "Dinko", "Tonči", "Veselko",
        
        // Greek
        "Georgios", "Nikolaos", "Dimitrios", "Konstantinos", "Panagiotis",
        "Athanasios", "Ioannis", "Christos", "Vasilis", "Nikos", "Stefanos",
        
        // Turkish
        "Mehmet", "Mustafa", "Ali", "Hasan", "Ibrahim", "Ahmet", "Fatih",
        "Kadir", "Selim", "Recep", "Erkan", "Cem", "Serkan", "Yusuf",
        
        // Russian
        "Sergei", "Vladimir", "Aleksandr", "Andrei", "Nikolai", "Mikhail",
        "Dmitri", "Yuri", "Igor", "Boris", "Leonid", "Pavel", "Viktor"
    };
    
    // Last names from various cultures
    private static final String[] LAST_NAMES = {
        // German
        "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Wagner", "Becker",
        "Schäfer", "Schulze", "Hoffmann", "Schroeder", "Koch", "Bauer", "Richter",
        "Krüger", "Huber", "Kaiser", "Schmitt", "Groß", "Braun", "Hartmann",
        
        // English
        "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller",
        "Wilson", "Moore", "Taylor", "Anderson", "Thomas", "Jackson", "White",
        "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson",
        
        // Spanish
        "García", "Martínez", "González", "Rodríguez", "Hernández", "López",
        "Pérez", "Sánchez", "Moreno", "Jiménez", "Díaz", "Ramírez", "Reyes",
        "Cruza", "Vázquez", "Castro", "Domínguez", "Ruiz",
        
        // Italian
        "Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Colombo",
        "Rizzo", "Marino", "Gallo", "Conti", "De Luca", "Mancini", "Costa",
        "Giordano", "Barbieri", "Benedetti", "Moretti",
        
        // French
        "Dubois", "Durand", "Dupont", "Lambert", "Lefebvre", "Simon", "Laurent",
        "Lefevre", "Leroy", "Moreau", "Michel", "Bernard", "Thomas", "Robert",
        "Petit", "Duval", "Bertrand", "Roux",
        
        // Dutch
        "de Vries", "Janssen", "van der Berg", "Bakker", "Visser", "Smit",
        "Mertens", "Hermans", "Willems", "Peeters", "Becker", "Wouters",
        "Claes", "Michiels", "Verhagen",
        
        // Portuguese
        "Silva", "Santos", "Oliveira", "Pereira", "Marques", "Correia",
        "Sousa", "Pinto", "Costa", "Teixeira", "Lopes", "Ferreira", "Gomes",
        "Cunha", "Rocha", "Dias", "Barbosa",
        
        // Polish
        "Kowalski", "Nowak", "Wojcik", "Kaminski", "Lewandowski", "Gajewski",
        "Zielinski", "Piotrowski", "Wisniewski", "Dabrowski", "Chmielewski",
        
        // Czech
        "Svoboda", "Novotný", "Novák", "Kuchta", "Pokorný", "Moravec",
        "Šimek", "Valenta", "Hofmann", "Němec", "Janík",
        
        // Swedish
        "Ström", "Persson", "Ek", "Bergström", "Danielsson", "Johansson",
        "Andersson", "Svedberg", "Holm", "Engström", "Lindström",
        
        // Norwegian
        "Andersen", "Hansen", "Larsen", "Olsen", "Eriksen", "Jensen",
        "Johansen", "Petersen", "Sørensen", "Moen", "Hauge", "Strand",
        
        // Danish
        "Jensen", "Hansen", "Andersen", "Petersen", "Madsen", "Rasmussen",
        "Larsen", "Jørgensen", "Kristensen", "Olsen", "Sørensen",
        
        // Hungarian
        "Nagy", "Kovács", "Tóth", "Molnár", "Horváth", "Varga", "Szabo",
        "Balogh", "Kiss", "Bodnár", "Szilágyi",
        
        // Serbian
        "Marković", "Stanković", "Nikolić", "Simić", "Jovanović", "Milosavljević",
        "Pavlović", "Vasić", "Đorđević", "Ristić", "Adžić",
        
        // Croatian
        "Marković", "Horvat", "Horvath", "Vuković", "Jurić", "Mikulić",
        "Novak", "Pavlović", "Tomić", "Čović", "Kamenicki",
        
        // Greek
        "Papadopoulos", "Papadopoulou", "Nikolopoulos", "Dimitriou", "Georgiou",
        "Angelopoulos", "Economou", "Vlahopoulos", "Chronopoulos",
        
        // Turkish
        "Yılmaz", "Kaya", "Demirel", "Özdemir", "Akçay", "Çelik", "Güngör",
        "Demir", "Aslan", "Alkan", "Arslan", "Başaran",
        
        // Russian
        "Petrov", "Sokolov", "Lebedev", "Kozlov", "Orlov", "Volkov",
        "Smirnov", "Vasiliev", "Popov", "Egorov", "Mikhailov", "Antonov"
    };
    
    // Countries from different regions
    private static final String[] COUNTRIES = {
        // Europe
        "Germany", "France", "Spain", "Italy", "England", "Netherlands", "Belgium",
        "Portugal", "Poland", "Czech Republic", "Hungary", "Austria", "Switzerland",
        "Sweden", "Norway", "Denmark", "Finland", "Greece", "Turkey", "Serbia",
        "Croatia", "Romania", "Bulgaria", "Ukraine", "Russia", "Scotland",
        "Wales", "Northern Ireland", "Bosnia", "Slovenia", "Slovakia",
        
        // South America
        "Brazil", "Argentina", "Uruguay", "Paraguay", "Colombia", "Venezuela",
        "Peru", "Chile", "Ecuador", "Suriname", "Guyana",
        
        // North/Central America & Caribbean
        "Mexico", "United States", "Canada", "Jamaica", "Trinidad & Tobago",
        "Costa Rica", "El Salvador", "Honduras", "Nicaragua", "Panama",
        "Dominican Republic", "Haiti", "Belize", "Guatemala",
        
        // Africa
        "Nigeria", "Egypt", "South Africa", "Cameroon", "Ghana", "Ivory Coast",
        "Senegal", "Morocco", "Algeria", "Tunisia", "Mali", "Kenya", "Zambia",
        "Zimbabwe", "Ethiopia", "Angola", "Uganda", "Congo", "Benin", "Gabon",
        
        // Asia
        "Japan", "South Korea", "China", "Vietnam", "Thailand", "Indonesia",
        "Malaysia", "Singapore", "Philippines", "Cambodia", "Myanmar",
        "India", "Pakistan", "Bangladesh", "Iran", "Iraq", "Saudi Arabia",
        "United Arab Emirates", "Qatar", "Bahrain", "Oman", "Yemen",
        
        // Oceania
        "Australia", "New Zealand", "Fiji", "Samoa", "Papua New Guinea"
    };
    
    /**
     * Generates a random player name and country.
     * @return A string array containing [firstName lastName, country]
     */
    public static String[] generatePlayerNameAndCountry() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
        
        return new String[]{firstName + " " + lastName, country};
    }
    
    /**
     * Generates a random country.
     * @return A random country name
     */
    public static String generateCountry() {
        return COUNTRIES[random.nextInt(COUNTRIES.length)];
    }
    
    /**
     * Generates a random player name.
     * @return A random first and last name
     */
    public static String generatePlayerName() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }
}
