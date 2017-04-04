import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Time;
import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.*; 
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.List;
import java.util.TreeSet;
import java.util.Collections;

import javax.swing.*;

public class TSP {

	private static final int cityShiftAmount = 60; //DO NOT CHANGE THIS.
	
    /**
     * How many cities to use.
     */
    protected static int cityCount;

    /**
     * How many chromosomes to use.
     */
    protected static int populationSize = 100; //DO NOT CHANGE THIS.

    /**
     * The part of the population eligable for mating.
     */
    protected static int matingPopulationSize;

    /**
     * The part of the population selected for mating.
     */
    protected static int selectedParents;

    /**
     * The current generation
     */
    protected static int generation;

    /**
     * The list of cities (with current movement applied).
     */
    protected static City[] cities;
    
    /**
     * The list of cities that will be used to determine movement.
     */
    private static City[] originalCities;

    /**
     * The list of chromosomes.
     */
    protected static Chromosome[] chromosomes;

    /**
    * Frame to display cities and paths
    */
    private static JFrame frame;

    /**
     * Integers used for statistical data
     */
    private static double min;
    private static double avg;
    private static double max;
    private static double sum;
    private static double genMin;

    /**
     * Width and Height of City Map, DO NOT CHANGE THESE VALUES!
     */
    private static int width = 600;
    private static int height = 600;


    private static Panel statsArea;
    private static TextArea statsText;


    /*
     * Writing to an output file with the costs.
     */
    private static void writeLog(String content) {
        String filename = "results.out";
        FileWriter out;

        try {
            out = new FileWriter(filename, true);
            out.write(content + "\n");
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     *  Deals with printing same content to System.out and GUI
     */
    private static void print(boolean guiEnabled, String content) {
        if(guiEnabled) {
            statsText.append(content + "\n");
        }

        System.out.println(content);
    }

    public static void checkSanity() {
        for (Chromosome chromo: chromosomes) {
            if (chromo.hasDuplicates()) {
                throw new IllegalArgumentException("Duplicated chromosomes!");
            }
        }
    }

    public static void inversionMutation(int mutantPool) {
        LinkedList<Chromosome> mutants = new LinkedList<>();
        Chromosome.sortChromosomes(chromosomes, chromosomes.length);
        for (int i = 0; i < mutantPool; i++) {
            Chromosome c = chromosomes[chromosomes.length - i - 1];
            mutants.add(c);
            mutants.add(c.mutate(cities));
        }
        List<Chromosome> bestMutants = mutants.stream()
               .sorted((c1, c2) -> {
                   return Double.compare(c1.getCost(), c2.getCost());
               })
               .limit(mutantPool)
               .collect(Collectors.toList());
        int idx = 0;
        for (Chromosome mutant : bestMutants) {
            chromosomes[chromosomes.length - idx - 1] = mutant;
            idx += 1;
        }
    }

    public static ArrayList<Chromosome> getTournamentSelection(int k, int tournaments) {
        assert(tournaments % 2 == 0);
        ArrayList<Chromosome> breaders = new ArrayList<>();
        int length = chromosomes.length;
        Random r = new Random();
        for (int i = 0; i < tournaments; i++) {
            ArrayList<Chromosome> tournament = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                int idx = r.nextInt(length);
                tournament.add(chromosomes[idx]);
            }
            breaders.add(
                    tournament
                        .stream()
                        .max((c1, c2) -> Double.compare(c1.getCost(), c2.getCost()))
                        .get()
            );
        }
        return breaders;
    }

    public static ArrayList<Chromosome> getFitnessProporitional(int k, ArrayList<Chromosome> chromos) {
        ArrayList<Chromosome> chosen = new ArrayList<>();
        int length = chromos.size();
        chromos = chromos.stream()
               .sorted((c1, c2) -> {
                   return Double.compare(c1.getCost(), c2.getCost());
               })
               .collect(Collectors.toCollection(ArrayList::new));
        Random r = new Random();
        while (chosen.size() < k) {
            for (int i = 0; i < chromos.size(); i++) {
                double p = (length - i - 1.0) / (double) length;
                double d = r.nextDouble();
                if (d < p) {
                    chosen.add(chromos.get(i));
                    chromos.remove(i);
                    if (k <= chosen.size()) {
                        break;
                    }
                }
            }

        }
        return chosen;
    }

    public static ArrayList<Chromosome> getElite(int number, double eliteInfluence, ArrayList<Chromosome> population) {
        ArrayList<Chromosome> elite = population.stream()
                         .sorted((p1, p2) -> Double.compare(p1.getCost(), p2.getCost()))
                         .limit(number)
                         .collect(Collectors.toCollection(ArrayList::new));
        Random r = new Random();
        for (int i = 0; i < number; i++) {
            double p = r.nextDouble();
            if (eliteInfluence < p) {
                int idx = r.nextInt(population.size());
                elite.set(i, population.get(idx));
            }
        }
        return elite;

    }

    public static void eliteTournament() {
        int numberParents = 8;
        int tournamentDiversity = 4;
        ArrayList<Chromosome> offspring = getTournamentSelection(tournamentDiversity, numberParents);
        for (int i = 0; i < numberParents; i += 2) {
            Chromosome p1 = offspring.get(i);
            Chromosome p2 = offspring.get(i+1);
            offspring.add(p1.pmx(cities, p2));
            offspring.add(p2.pmx(cities, p1));
        }
        for (Chromosome c: chromosomes) {
            offspring.add(c);
        }
        offspring = getElite(chromosomes.length, 0.7, offspring);
        for (int i = 0; i < chromosomes.length; i++) {
            chromosomes[i] = offspring.get(i);
        }
    }

    public static ArrayList<Chromosome> getBest(int number) {
        ArrayList<Chromosome> best = new ArrayList<>();
        Chromosome.sortChromosomes(chromosomes, chromosomes.length);
        for (int i = 0 ; i < number; i++) {
            best.add(chromosomes[i]);
        }
        return best;
    }

    public static ArrayList<Chromosome> getWorst(int number) {
        ArrayList<Chromosome> worst = new ArrayList<>();
        int length = chromosomes.length;
        Chromosome.sortChromosomes(chromosomes, length);
        for (int i = 0 ; i < number; i++) {
            worst.add(chromosomes[length - i - 1]);
        }
        return worst;
    }

    public static ArrayList<Chromosome> getRandom(int start, int end, int number) {
        TreeSet<Integer> chosen = new TreeSet<>();
        assert(0 < (end - start));
        assert(number < (end - start));
        Random r = new Random();
        int d = end - start;
        ArrayList<Chromosome> randoms = new ArrayList<>();
        while (chosen.size() < number) {
            int idx = start + r.nextInt(d);
            if (!chosen.contains(idx)) {
                chosen.add(idx);
                randoms.add(chromosomes[idx]);
            }
        }
        return randoms;
    }

    public static ArrayList<Chromosome> sortByFitness(ArrayList<Chromosome> zomes) {
        return zomes.stream()
                    .sorted((c1, c2) -> {
                        return Double.compare(c1.getCost(), c2.getCost());
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<Chromosome> getTopN(ArrayList<Chromosome> zomes, int n) {
        return zomes.stream()
                    .sorted((c1, c2) -> {
                        return Double.compare(c1.getCost(), c2.getCost());
                    })
                    .limit(n)
                    .collect(Collectors.toCollection(ArrayList::new));
    }

    public static void eliteSelection() {
        int length = chromosomes.length;
        int numberBest = 10;
        int numberAverage = 4;
        int numberWorst = 4;
        ArrayList<Chromosome> best = getBest(numberBest);
        ArrayList<Chromosome> children = new ArrayList<Chromosome>();
        ArrayList<Chromosome> average = getRandom(numberBest, length-numberWorst, numberAverage);
        best.addAll(average);
        Collections.shuffle(best); // shuffle to increase genetic diversity
        for (int i = 0; i < best.size(); i+=2) {
            Chromosome p1 = best.get(i);
            Chromosome p2 = best.get(i+1);
            Chromosome child1 = p1.pmx(cities, p2);
            Chromosome child2 = p2.pmx(cities, p1);
            /*
            p1.printOut();
            p2.printOut();
            child1.printOut();
            child2.printOut();
            */
            children.add(child1);
            children.add(child2);
        }
        children.addAll(getWorst(numberWorst));
        children = getTopN(children, numberWorst);
        for (int i = 0; i < numberWorst; i++) {
            chromosomes[length - i - 1] = children.get(i);
        }
    }

    public static void evolve() {
        inversionMutation(100);
        checkSanity();
    }

    /**
     * Update the display
     */
    public static void updateGUI() {
        Image img = frame.createImage(width, height);
        Graphics g = img.getGraphics();
        FontMetrics fm = g.getFontMetrics();

        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);

        if (true && (cities != null)) {
            for (int i = 0; i < cityCount; i++) {
                int xpos = cities[i].getx();
                int ypos = cities[i].gety();
                g.setColor(Color.green);
                g.fillOval(xpos - 5, ypos - 5, 10, 10);
                
                //// SHOW Outline of movement boundary
                // xpos = originalCities[i].getx();
                // ypos = originalCities[i].gety();
                // g.setColor(Color.darkGray);
                // g.drawLine(xpos + cityShiftAmount, ypos, xpos, ypos + cityShiftAmount);
                // g.drawLine(xpos, ypos + cityShiftAmount, xpos - cityShiftAmount, ypos);
                // g.drawLine(xpos - cityShiftAmount, ypos, xpos, ypos - cityShiftAmount);
                // g.drawLine(xpos, ypos - cityShiftAmount, xpos + cityShiftAmount, ypos);
            }

            g.setColor(Color.gray);
            for (int i = 0; i < cityCount; i++) {
                int icity = chromosomes[0].getCity(i);
                if (i != 0) {
                    int last = chromosomes[0].getCity(i - 1);
                    g.drawLine(
                        cities[icity].getx(),
                        cities[icity].gety(),
                        cities[last].getx(),
                        cities[last].gety());
                }
            }
                        
            int homeCity = chromosomes[0].getCity(0);
            int lastCity = chromosomes[0].getCity(cityCount - 1);
                        
            //Drawing line returning home
            g.drawLine(
                    cities[homeCity].getx(),
                    cities[homeCity].gety(),
                    cities[lastCity].getx(),
                    cities[lastCity].gety());
        }
        frame.getGraphics().drawImage(img, 0, 0, frame);
    }

    private static City[] LoadCitiesFromFile(String filename, City[] citiesArray) {
        ArrayList<City> cities = new ArrayList<City>();
        try 
        {
            FileReader inputFile = new FileReader(filename);
            BufferedReader bufferReader = new BufferedReader(inputFile);
            String line;
            while ((line = bufferReader.readLine()) != null) { 
                String [] coordinates = line.split(", ");
                cities.add(new City(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1])));
            }

            bufferReader.close();

        } catch (Exception e) {
            System.out.println("Error while reading file line by line:" + e.getMessage());                      
        }
        
        citiesArray = new City[cities.size()];
        return cities.toArray(citiesArray);
    }

    private static City[] MoveCities(City[]cities) {
    	City[] newPositions = new City[cities.length];
        Random randomGenerator = new Random();

        for(int i = 0; i < cities.length; i++) {
        	int x = cities[i].getx();
        	int y = cities[i].gety();
        	
            int position = randomGenerator.nextInt(5);
            
            if(position == 1) {
            	y += cityShiftAmount;
            } else if(position == 2) {
            	x += cityShiftAmount;
            } else if(position == 3) {
            	y -= cityShiftAmount;
            } else if(position == 4) {
            	x -= cityShiftAmount;
            }
            
            newPositions[i] = new City(x, y);
        }
        
        return newPositions;
    }

    public static void main(String[] args) {
        DateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        String currentTime  = df.format(today);

        int runs;
        boolean display = false;
        String formatMessage = "Usage: java TSP 1 [gui] \n java TSP [Runs] [gui]";

        if (args.length < 1) {
            System.out.println("Please enter the arguments");
            System.out.println(formatMessage);
            display = false;
        } else {

            if (args.length > 1) {
                display = true; 
            }

            try {
                cityCount = 50;
                populationSize = 100;
                runs = Integer.parseInt(args[0]);

                if(display) {
                    frame = new JFrame("Traveling Salesman");
                    statsArea = new Panel();

                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.pack();
                    frame.setSize(width + 300, height);
                    frame.setResizable(false);
                    frame.setLayout(new BorderLayout());
                    
                    statsText = new TextArea(35, 35);
                    statsText.setEditable(false);

                    statsArea.add(statsText);
                    frame.add(statsArea, BorderLayout.EAST);
                    
                    frame.setVisible(true);
                }


                min = 0;
                avg = 0;
                max = 0;
                sum = 0;

                originalCities = cities = LoadCitiesFromFile("CityList.txt", cities);

                writeLog("Run Stats for experiment at: " + currentTime);
                for (int y = 1; y <= runs; y++) {
                    genMin = 0;
                    print(display,  "Run " + y + "\n");

                // create the initial population of chromosomes
                    chromosomes = new Chromosome[populationSize];
                    for (int x = 0; x < populationSize; x++) {
                        chromosomes[x] = new Chromosome(cities);
                    }

                    generation = 0;
                    double thisCost = 0.0;

                    while (generation < 100) {
                        evolve();
                        if(generation % 5 == 0 ) 
                            cities = MoveCities(originalCities); //Move from original cities, so they only move by a maximum of one unit.
                        generation++;

                        Chromosome.sortChromosomes(chromosomes, populationSize);
                        double cost = chromosomes[0].getCost();
                        thisCost = cost;

                        if (thisCost < genMin || genMin == 0) {
                            genMin = thisCost;
                        }
                        
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMinimumFractionDigits(2);
                        nf.setMinimumFractionDigits(2);

                        print(display, "Gen: " + generation + " Cost: " + (int) thisCost);

                        if(display) {
                            updateGUI();
                        }
                    }

                    writeLog(genMin + "");

                    if (genMin > max) {
                        max = genMin;
                    }

                    if (genMin < min || min == 0) {
                        min = genMin;
                    }

                    sum +=  genMin;

                    print(display, "");
                }

                avg = sum / runs;
                print(display, "Statistics after " + runs + " runs");
                print(display, "Solution found after " + generation + " generations." + "\n");
                print(display, "Statistics of minimum cost from each run \n");
                print(display, "Lowest: " + min + "\nAverage: " + avg + "\nHighest: " + max + "\n");

            } catch (NumberFormatException e) {
                System.out.println("Please ensure you enter integers for cities and population size");
                System.out.println(formatMessage);
            }
        }
    }
}
