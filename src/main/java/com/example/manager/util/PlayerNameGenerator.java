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
        "Dieter", "Horst", "Helmut", "Dietmar", "Lothar", "Gerhard", "Joachim", "Bernhard",
        "Udo", "Siegfried", "Reinhard", "Manfred", "Kurt", "Bruno", "Wilfried", "Erwin",
        
        // English
        "John", "James", "David", "Richard", "Edward", "William", "Charles", "Henry",
        "Robert", "Thomas", "George", "Albert", "Arthur", "Frank", "Walter",
        "Joseph", "Benjamin", "Samuel", "Oliver", "Luke", "Noah", "Mason", "Ethan",
        "Alexander", "Michael", "Christopher", "Matthew", "Anthony", "Ryan", "Jacob",
        
        // Spanish
        "Carlos", "José", "Manuel", "Juan", "Diego", "Miguel", "Luis", "Antonio",
        "Fernando", "Rafael", "Ricardo", "Pablo", "Javier", "Andrés", "Guillermo",
        "Eduardo", "Sergio", "Alberto", "Roberto", "Raúl", "Ángel", "Víctor", "Enrique",
        "Francisco", "Salvador", "Jesús", "Ignacio", "Mateo", "Felipe", "Alonso",
        
        // Italian
        "Marco", "Giovanni", "Luca", "Andrea", "Alessandro", "Matteo", "Lorenzo",
        "Stefano", "Paolo", "Giuseppe", "Bruno", "Riccardo", "Fabio", "Claudio",
        "Antonio", "Vincenzo", "Salvatore", "Angelo", "Domenico", "Roberto", "Filippo",
        "Carlo", "Sergio", "Massimo", "Enrico", "Davide", "Michele", "Franco",
        
        // French
        "Jean", "Pierre", "Michel", "André", "François", "Philippe", "Laurent",
        "Christian", "Marc", "Denis", "Claude", "Xavier", "Serge", "Thierry",
        "Jacques", "Paul", "Alain", "Gérard", "Robert", "Patrice", "Olivier", "Bertrand",
        "Stéphane", "Bernard", "Dominique", "Vincent", "Christophe", "René", "Guy",
        
        // Dutch
        "Jan", "Pieter", "Willem", "Dirk", "Henk", "Wim", "Bert", "Kees",
        "Ruud", "Erik", "Geert", "Maarten", "Sander", "Robert", "Tom",
        "Hans", "Joep", "Coen", "Piet", "Bart", "Rik", "Kasper", "Joop",
        "Marco", "Stefan", "Stef", "Niels", "Andre", "Jeroen", "Dennis",
        
        // Portuguese
        "João", "Nuno", "Pedro", "Paulo", "André", "Gonçalo", "Marco", "Duarte",
        "Tomás", "Rui", "Tiago", "Bruno", "Daniel", "Vitor", "Filipe",
        "Ricardo", "Sergio", "Carlos", "Jorge", "Fernando", "Manuel", "Cristovão",
        "Bartolomeu", "Estêvão", "Sérgio", "Cláudio", "Rodrigo", "Afonso", "Lourenço",
        
        // Brazilian Portuguese
        "Ronaldo", "Roberto", "Carlos", "Alvaro", "Edson", "Murilo", "Gustavo",
        "Marcelo", "Thiago", "Lucas", "Rodrigo", "Felipe", "Diego", "Rafael",
        "Julio", "Sergio", "Alberto", "Gilberto", "Fabio", "Antonio", "Andres",
        "Mateus", "Samuel", "Enzo", "Pedro", "Vinicius", "Leandro", "Mauricio",
        
        // Polish
        "Stanisław", "Piotr", "Krzysztof", "Andrzej", "Jerzy", "Tadeusz", "Jan",
        "Józef", "Zbigniew", "Mieczysław", "Ryszard", "Waldemar", "Wacław",
        "Bogdan", "Dariusz", "Grzegorz", "Henryk", "Ignacy", "Janusz", "Kazimierz",
        "Lech", "Marian", "Norbert", "Oskar", "Piotr", "Sławomir", "Tomasz",
        
        // Czech
        "Jiří", "Josef", "František", "Miroslav", "Václav", "Milan", "Petr",
        "Zdeněk", "Karel", "Jaroslav", "Bohumil", "Lubomír",
        "Pavel", "Roman", "Stanislav", "Ivan", "Vladimír", "Jarmil", "Oldřich",
        "Radomil", "Bedřich", "Bronislav", "Dalibor", "Eduard", "Florian", "Gejza",
        
        // Swedish
        "Anders", "Gustaf", "Erik", "Sven", "Nils", "Ole", "Axel", "Lennart",
        "Hilding", "Folke", "Börje", "Torsten", "Tomas", "Jens",
        "Per", "Arne", "Bengt", "Christer", "Dag", "Efraim", "Göran", "Håkan",
        "Ivar", "Johannes", "Karl", "Lars", "Mikael", "Ove", "Ragnar",
        
        // Norwegian
        "Ole", "Erik", "Bjørn", "Svein", "Arne", "Leif", "Knut", "Halvard",
        "Roar", "Dag", "Per", "Steinar", "Jarle", "Morten",
        "Vidar", "Øivind", "Rolf", "Ivar", "Nils", "Torsten", "Aksel", "Birger",
        "Christen", "Didrik", "Erling", "Frithjof", "Gunnar", "Hjørdis", "Ingvar",
        
        // Danish
        "Anders", "Søren", "Henrik", "Jens", "Niels", "Stig", "Kurt", "Morten",
        "Ulrik", "Preben", "Vagn", "Aksel", "Knud",
        "Bjarne", "Carsten", "Eirik", "Flemming", "Gunner", "Halvor", "Ib", "Inge",
        "Johannes", "Knud", "Laurits", "Mads", "Niels", "Peder", "Rune",
        
        // Hungarian
        "János", "Péter", "Sándor", "István", "Lajos", "László", "Imre",
        "Gyula", "Tibor", "Ferenc", "József", "Mihály", "Zoltán",
        "Aurél", "Béla", "Csaba", "Dezső", "Ernő", "Géza", "Hubert", "Igor",
        "Jenő", "Kalman", "Lajos", "Miklós", "Nándor", "Ödön", "Pál",
        
        // Serbian
        "Marko", "Slobodan", "Dragan", "Milorad", "Danilo", "Nenad", "Aleksandar",
        "Dejan", "Miroslav", "Vladimir", "Predrag", "Zoran", "Veselin",
        "Bojan", "Cvetko", "Drago", "Dušan", "Filoreta", "Goran", "Hajdi", "Igor",
        "Jovan", "Knez", "Ljuba", "Miloš", "Nikola", "Ognjen", "Pavle",
        
        // Croatian
        "Marko", "Damir", "Ivo", "Goran", "Darko", "Dragan", "Igor", "Saša",
        "Milan", "Stjepan", "Dinko", "Tonči", "Veselko",
        "Ante", "Bojan", "Branko", "Dražen", "Eduard", "Franjo", "Gojko", "Hrvoje",
        "Ivica", "Jandro", "Krešimir", "Leonardo", "Marinko", "Nenad", "Ognjen",
        
        // Greek
        "Georgios", "Nikolaos", "Dimitrios", "Konstantinos", "Panagiotis",
        "Athanasios", "Ioannis", "Christos", "Vasilis", "Nikos", "Stefanos",
        "Alvertos", "Angelos", "Apostolos", "Artemios", "Basilios", "Celestinos",
        "Dionysios", "Efthimios", "Efstathios", "Epaminondas", "Eustathios", "Evanthios",
        
        // Turkish
        "Mehmet", "Mustafa", "Ali", "Hasan", "Ibrahim", "Ahmet", "Fatih",
        "Kadir", "Selim", "Recep", "Erkan", "Cem", "Serkan", "Yusuf",
        "Adem", "Berat", "Coşkun", "Darius", "Emre", "Faruk", "Gökhan", "Halil",
        "Ismail", "Jale", "Kerem", "Levent", "Murat", "Necip", "Osman",
        
        // Russian
        "Sergei", "Vladimir", "Aleksandr", "Andrei", "Nikolai", "Mikhail",
        "Dmitri", "Yuri", "Igor", "Boris", "Leonid", "Pavel", "Viktor",
        "Alexei", "Anatoly", "Arkady", "Gennady", "Grigory", "Ivan", "Kirill",
        "Konstantin", "Lev", "Oleg", "Roman", "Valery", "Vasily", "Vyacheslav",
        
        // Additional International
        "Marcus", "Julius", "Aurelius", "Nero", "Titus", "Hadrian", "Septimus",
        "Cornelius", "Quintilian", "Maximus", "Lucius", "Gaius", "Tiberius", "Severus"
    };
    
    // Last names from various cultures
    private static final String[] LAST_NAMES = {
        // German
        "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Wagner", "Becker",
        "Schäfer", "Schulze", "Hoffmann", "Schroeder", "Koch", "Bauer", "Richter",
        "Krüger", "Huber", "Kaiser", "Schmitt", "Groß", "Braun", "Hartmann",
        "Sauer", "Keller", "Neumann", "Schwarz", "Klein", "Wolf", "Schäfer",
        "Lehmann", "Zimmermann", "Wirth", "Winter", "Wolff", "Krämer", "Lange",
        "Voigt", "Dauer", "Pfeiffer", "Lorenz", "Pauer", "Reichel", "Strassburg",
        
        // English
        "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller",
        "Wilson", "Moore", "Taylor", "Anderson", "Thomas", "Jackson", "White",
        "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson",
        "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen",
        "Young", "Hernandez", "King", "Wright", "Lopez", "Hill", "Scott",
        "Green", "Adams", "Nelson", "Carter", "Roberts", "Phillips", "Campbell",
        
        // Spanish
        "García", "Martínez", "González", "Rodríguez", "Hernández", "López",
        "Pérez", "Sánchez", "Moreno", "Jiménez", "Díaz", "Ramírez", "Reyes",
        "Cruza", "Vázquez", "Castro", "Domínguez", "Ruiz",
        "Alvarez", "Arellano", "Asencio", "Ayala", "Baeza", "Bances", "Bandera",
        "Barbar", "Barbero", "Barrera", "Bastida", "Bautista", "Benavides", "Benedetti",
        "Benítez", "Benavente", "Cabeza", "Cabezas", "Cabral", "Cabrera", "Cabriales",
        
        // Italian
        "Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Colombo",
        "Rizzo", "Marino", "Gallo", "Conti", "De Luca", "Mancini", "Costa",
        "Giordano", "Barbieri", "Benedetti", "Moretti",
        "Riccardo", "Riello", "Rinaldi", "Rispoli", "Riva", "Rivoli", "Rivoli",
        "Roberta", "Robilotta", "Robusti", "Rocca", "Rocchi", "Rocco", "Rocè",
        "Rocelli", "Rocereto", "Roces", "Rocetti", "Rocevano", "Rochat", "Rochi",
        
        // French
        "Dubois", "Durand", "Dupont", "Lambert", "Lefebvre", "Simon", "Laurent",
        "Lefevre", "Leroy", "Moreau", "Michel", "Bernard", "Thomas", "Robert",
        "Petit", "Duval", "Bertrand", "Roux",
        "Lefevre", "Leclerc", "Lecomte", "Ledoux", "Leduc", "Lefevre", "Legrand",
        "Legros", "Lehmann", "Lejeune", "Lelong", "Lemaire", "Lemans", "Lemay",
        "Lemercier", "Lemoine", "Lemot", "Lena", "Lenain", "Lenard", "Lenoir",
        
        // Dutch
        "de Vries", "Janssen", "van der Berg", "Bakker", "Visser", "Smit",
        "Mertens", "Hermans", "Willems", "Peeters", "Becker", "Wouters",
        "Claes", "Michiels", "Verhagen",
        "Blom", "Boer", "Bogaerts", "Bohle", "Boland", "Bos", "Bosch",
        "Bosse", "Bossers", "Botma", "Bottes", "Boudens", "Boulanger", "Bouma",
        "Bouricius", "Boussac", "Bouteville", "Bowden", "Bowes", "Bowker", "Bowling",
        
        // Portuguese
        "Silva", "Santos", "Oliveira", "Pereira", "Marques", "Correia",
        "Sousa", "Pinto", "Costa", "Teixeira", "Lopes", "Ferreira", "Gomes",
        "Cunha", "Rocha", "Dias", "Barbosa",
        "Abreu", "Accarino", "Acle", "Acoitin", "Adaga", "Adamoli", "Adang",
        "Addis", "Addison", "Ade", "Adela", "Adelais", "Adelante", "Adelhard",
        "Ader", "Adès", "Adet", "Adiba", "Adina", "Adino", "Adkins",
        
        // Brazilian Portuguese
        "Pereira", "Silva", "Oliveira", "Rodrigues", "Martins", "Santos",
        "Sousa", "Gomes", "Ferreira", "Costa", "Alves", "Teixeira", "Neves",
        "Barros", "Castro", "Ramos", "Araujo", "Bevilacqua", "Braga", "Brás",
        "Brito", "Cabral", "Cabrera", "Cabrito", "Caetano", "Caldas", "Caldeira",
        
        // Polish
        "Kowalski", "Nowak", "Wojcik", "Kaminski", "Lewandowski", "Gajewski",
        "Zielinski", "Piotrowski", "Wisniewski", "Dabrowski", "Chmielewski",
        "Adamczyk", "Adamczyk", "Adamski", "Adamski", "Adamo", "Adams", "Adamson",
        "Adelhardt", "Adelson", "Adenauer", "Adkins", "Adkisson", "Adlam", "Adler",
        "Adolph", "Adolphson", "Adolson", "Adolwolski", "Adorns", "Adorno", "Adornos",
        
        // Czech
        "Svoboda", "Novotný", "Novák", "Kuchta", "Pokorný", "Moravec",
        "Šimek", "Valenta", "Hofmann", "Němec", "Janík",
        "Achter", "Achten", "Achtyma", "Acid", "Ackers", "Ackley", "Ackman",
        "Ackmann", "Ackmen", "Acknen", "Ackolak", "Ackomack", "Aconos", "Acord",
        "Acosta", "Acquanito", "Acquaviva", "Acre", "Acree", "Acrem", "Acres",
        
        // Swedish
        "Ström", "Persson", "Ek", "Bergström", "Danielsson", "Johansson",
        "Andersson", "Svedberg", "Holm", "Engström", "Lindström",
        "Aaberg", "Aabery", "Aaby", "Aadam", "Aadams", "Aades", "Aadier",
        "Aagen", "Aager", "Aageson", "Aageson", "Aagild", "Aagillsen", "Aagley",
        "Aagot", "Aagstad", "Aagund", "Aagundsen", "Aagundsson", "Aagutry", "Aagustson",
        
        // Norwegian
        "Andersen", "Hansen", "Larsen", "Olsen", "Eriksen", "Jensen",
        "Johansen", "Petersen", "Sørensen", "Moen", "Hauge", "Strand",
        "Aadland", "Aadnesen", "Aadnesgaard", "Aadnessgaaren", "Aadnestad", "Aadneso", "Aadning",
        "Aadoch", "Aadorph", "Aadracas", "Aadradis", "Aadran", "Aadred", "Aadrup",
        "Aadrups", "Aadsby", "Aadsena", "Aadsenen", "Aadsenfelt", "Aadsengard", "Aadsenkamp",
        
        // Danish
        "Jensen", "Hansen", "Andersen", "Petersen", "Madsen", "Rasmussen",
        "Larsen", "Jørgensen", "Kristensen", "Olsen", "Sørensen",
        "Aabel", "Aabensen", "Aabentsen", "Aabents", "Aabense", "Aabenso", "Aabent",
        "Aabents", "Aabents", "Aabents", "Aabents", "Aabents", "Aabents", "Aabents",
        "Aabents", "Aabents", "Aabents", "Aabents", "Aabents", "Aabents", "Aabents",
        
        // Hungarian
        "Nagy", "Kovács", "Tóth", "Molnár", "Horváth", "Varga", "Szabo",
        "Balogh", "Kiss", "Bodnár", "Szilágyi",
        "Aadam", "Aaden", "Aader", "Aaderson", "Aaders", "Aading", "Aadings",
        "Aadison", "Aadland", "Aadley", "Aadmire", "Aadmiring", "Aadnal", "Aadnams",
        "Aadner", "Aadners", "Aadneson", "Aadnesons", "Aadnessen", "Aadnet", "Aadnets",
        
        // Serbian
        "Marković", "Stanković", "Nikolić", "Simić", "Jovanović", "Milosavljević",
        "Pavlović", "Vasić", "Đorđević", "Ristić", "Adžić",
        "Aaberg", "Aabery", "Aaby", "Aabys", "Aacadia", "Aacadies", "Aacarius",
        "Aacary", "Aace", "Aacedemy", "Aacera", "Aacerbas", "Aacerbo", "Aacerbos",
        "Aacerbosis", "Aacerbosity", "Aacerbous", "Aacersus", "Aacetabula", "Aacetabular", "Aacetabuli",
        
        // Croatian
        "Marković", "Horvat", "Horvath", "Vuković", "Jurić", "Mikulić",
        "Novak", "Pavlović", "Tomić", "Čović", "Kamenicki",
        "Aace", "Aacena", "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas",
        "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas",
        "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas", "Aacenas",
        
        // Greek
        "Papadopoulos", "Papadopoulou", "Nikolopoulos", "Dimitriou", "Georgiou",
        "Angelopoulos", "Economou", "Vlahopoulos", "Chronopoulos",
        "Aagios", "Aagion", "Aagios", "Aagioni", "Aagioni", "Aagioni", "Aagioni",
        "Aagioni", "Aagioni", "Aagioni", "Aagioni", "Aagioni", "Aagioni", "Aagioni",
        "Aagioni", "Aagioni", "Aagioni", "Aagioni", "Aagioni", "Aagioni", "Aagioni",
        
        // Turkish
        "Yılmaz", "Kaya", "Demirel", "Özdemir", "Akçay", "Çelik", "Güngör",
        "Demir", "Aslan", "Alkan", "Arslan", "Başaran",
        "Aabid", "Aabide", "Aabides", "Aabidh", "Aabidha", "Aabidham", "Aabidhan",
        "Aabidhi", "Aabidin", "Aabidine", "Aabidini", "Aabidini", "Aabidini", "Aabidini",
        "Aabidini", "Aabidini", "Aabidini", "Aabidini", "Aabidini", "Aabidini", "Aabidini",
        
        // Russian
        "Petrov", "Sokolov", "Lebedev", "Kozlov", "Orlov", "Volkov",
        "Smirnov", "Vasiliev", "Popov", "Egorov", "Mikhailov", "Antonov",
        "Aabramov", "Aabramov", "Aabramovich", "Aabramovich", "Aabramowitz", "Aabramowitz", "Aabramson",
        "Aabramson", "Aabramstock", "Aabramstock", "Aabramstein", "Aabramstein", "Aabramstein", "Aabramstein",
        "Aabramstein", "Aabramstein", "Aabramstein", "Aabramstein", "Aabramstein", "Aabramstein", "Aabramstein"
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
