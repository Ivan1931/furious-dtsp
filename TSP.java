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

    /**
     * WRITTEN - checks that we do not have a chromosome with duplicate elements
     **/
    public static void checkSanity() {
        for (Chromosome chromo: chromosomes) {
            if (chromo.hasDuplicates()) {
                throw new IllegalArgumentException("Duplicated chromosomes!");
            }
        }
    }

    /**
     * WRITTEN - Performs inversion mutation by replacing the worst element with
     * the best element. Performs this mutationRate times.
     */
    public static void inversionMutation(int mutationRate) {
        Chromosome.sortChromosomes(chromosomes, chromosomes.length);
        int last_idx = chromosomes.length - 1;
        for (int i = 0; i < mutationRate; i++) {
            Chromosome worst = chromosomes[last_idx];
            Chromosome best = chromosomes[0];
            Chromosome mutant = best.mutate(cities);
            chromosomes[last_idx] = mutant;
            for (int k = chromosomes.length - 1; 1 <= k; k--) {
                if (chromosomes[k].getCost() < chromosomes[k-1].getCost()) {
                    Chromosome temp = chromosomes[k];
                    chromosomes[k] = chromosomes[k-1];
                    chromosomes[k-1] = temp;
                } else {
                    break;
                }
            }
        }
    }

    private static Random r = new Random();

    /**
     * Written performs tournament selection on our population
     */
    public static ArrayList<Chromosome> getTournamentSelection(int k, int tournaments) {
        assert(tournaments % 2 == 0);
        TreeSet<Integer> breaders = new TreeSet<>();
        int length = chromosomes.length;
        Random r = new Random();
        while (breaders.size() < tournaments) {
            ArrayList<Integer> tournament = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                int idx = r.nextInt(length);
                tournament.add(idx);
            }
            breaders.add(
                    tournament
                        .stream()
                        .min((c1, c2) -> {
                            return Double.compare(chromosomes[c1].getCost(), 
                                               chromosomes[c2].getCost());
                        })
                        .get()
            );
        }
        ArrayList<Chromosome> winners = new ArrayList<>();
        for (int idx : breaders) {
            winners.add(chromosomes[idx]);
        }
        return winners;
    }

    /**
     * WRITTEN - performs elitism selection on a list of chromosomes
     **/
    public static ArrayList<Chromosome> getRandomElite(int number, double elitismRate, ArrayList<Chromosome> offspring) {
        ArrayList<Chromosome> elite = offspring.stream().sorted((c1, c2) -> {
                return Double.compare(c1.getCost(), c2.getCost());
            })
            .limit(number)
            .collect(Collectors.toCollection(ArrayList::new));
        for (int i = 0; i < number; i++) {
            double d = r.nextDouble();
            if (elitismRate < d) {
                int idx = r.nextInt(offspring.size());
                elite.set(i, offspring.get(idx));
            }
        }
        return elite;
    }

    /**
     * WRITTEN - performs tournament selection and then survivor selection by a semi-randomised elitism process
     */
    public static void eliteTournament(int numberParents, int tournamentDiversity, double elitismRate) {
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
        offspring = getRandomElite(chromosomes.length, elitismRate, offspring);
        for (int i = 0; i < chromosomes.length; i++) {
            Chromosome replacement = offspring.get(i);
            chromosomes[i] = replacement;
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

    public static void evolve() {
        eliteTournament(30, 20, 0.7);
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
