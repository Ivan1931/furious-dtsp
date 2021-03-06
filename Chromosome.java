import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import java.util.TreeSet;

class Chromosome {

    /**
     * The list of cities, which are the genes of this chromosome.
     */
    protected int[] cityList;

    /**
     * The cost of following the cityList order of this chromosome.
     */
    protected double cost;

    /**
     * GIVEN + WRITTEN
     * Modified with a heuristic so that cities are typically spawned closer to each other
     * @param cities The order that this chromosome would visit the cities.
     */
    Chromosome(City[] cities) {
        Random generator = new Random();
        cityList = new int[cities.length];
        int[] copyCityList = new int[cities.length];
        //cities are visited based on the order of an integer representation [o,n] of each of the n cities.
        for (int x = 0; x < cities.length; x++) {
            cityList[x] = x;
        }

        //shuffle the order so we have a random initial order
        for (int y = 0; y < cityList.length; y++) {
            int temp = cityList[y];
            int randomNum = generator.nextInt(cityList.length);
            cityList[y] = cityList[randomNum];
            cityList[randomNum] = temp;
        }
        double cost1 = getCost();
        for (int i = 0 ;  i < cityList.length; i++) copyCityList[i] = cityList[i];

        for(int i = 1; i < cityList.length-1; i++) {
            int closest = i;
            int currentCity = cityList[i];
            double closestDistance = 100000.0;
            for (int j = i+1; j < cityList.length; j++) {
                int test = cityList[j];
                int testDistance = cities[test].proximity(cities[currentCity]);
                if (testDistance < closestDistance) {
                    closestDistance = testDistance;
                    closest = j;
                }
            }
            int temp = cityList[closest];
            cityList[closest] = cityList[i+1];
            cityList[i+1] = temp;
        }
        calculateCost(cities);

    }

    /**
     * Written - let's us construct cities with a preset citylist
     */
    Chromosome(City[] cities, int[] cityList) {
        this.cityList = cityList;
        this.calculateCost(cities);
    }

    /**
     * Calculate the cost of the specified list of cities.
     *
     * @param cities A list of cities.
     */
    void calculateCost(City[] cities) {
        cost = 0;
        for (int i = 0; i < cityList.length - 1; i++) {
            double dist = cities[cityList[i]].proximity(cities[cityList[i + 1]]);
            cost += dist;
        }
        cost += cities[cityList[0]].proximity(cities[cityList[cityList.length - 1]]); //Adding return home
    }

    private Random r = new Random();

    /**
     * Written - performs inversionMutation
     */
    public Chromosome mutate(City[] cities) {
        int length = cityList.length;
        boolean good = false;
        int[] x = randomRange(length);
        int a=x[0], b=x[1];
        int interval = b - a;
        int[] mutatedCityList = new int[length];
        for (int i = 0; i < length; i++) {
            mutatedCityList[i] = getCity(i);
        }
        for (int i = 0; i < interval/2; i++) {
            int temp = mutatedCityList[a+i];
            mutatedCityList[a+i] = mutatedCityList[b-i];
            mutatedCityList[b-i] = temp;
        }
        return new Chromosome(cities, mutatedCityList);
    }

    /**
     * WRITTEN - utility method to create random range
     */
    private int[] randomRange(int upperBound) {
        int start = 0, end = 0;
        do {
            start = r.nextInt(upperBound);
            end = r.nextInt(upperBound);
        } while(start == end);
        if (end < start) {
            int temp = start;
            start = end;
            end = temp;
        }
        return new int[] { start, end };
    }


    /**
     * WRITTEN - utility method to check if two chromosomes are equal by city
     **/
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Chromosome)) {
            return false;
        }
        Chromosome that = (Chromosome) obj;
        if (this.cityList.length  == that.cityList.length) {
            for (int i = 0; i < this.cityList.length; i++) {
                if (this.getCity(i) != that.getCity(i)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * WRITTEN
     * Performs partially mapped crossover between two cities
     **/
    public Chromosome pmx(City[] cities,  Chromosome mate) {
        int length = cityList.length;
        int[] x = randomRange(length);
        int[] newList = new int[length];
        /*
        if (this.hasDuplicates()) {
            throw new IllegalArgumentException("this has duplicates");
        }
        if (mate.hasDuplicates()) {
            throw new IllegalArgumentException("mate has duplicates");
        }
        */
        if (this.equals(mate)) {
            mate = mate.mutate(cities);
        }
        // Store all the things that we have already placed for easy access
        TreeSet<Integer> placed = new TreeSet<Integer>();
        // Place the middle segment
        //
        for (int i = x[0]; i <= x[1]; i++) {
            int candidate = getCity(i);
            newList[i] = candidate;
            placed.add(candidate);
        }
        for (int i = x[0]; i <= x[1]; i++) {
            int candidate = mate.getCity(i);
            if (!placed.contains(candidate)) {
                int searchFor = newList[i];
                for (int j = 0; j < length; j++) {
                    if (mate.getCity(j) == searchFor) {
                        newList[j] = candidate;
                        placed.add(candidate);
                        break;
                    }
                }
            }
        }
        /**
         * Copy all remaining unplaced elements from this into final list
         **/
        for (int i = 0; i < length; i++) {
            newList[i] = getCity(i);
        }
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (newList[i] == 0) {
                count+=1;
            }
        }
        return new Chromosome(cities, newList);
    }

    void printOut() {
        for (int city : cityList) {
            System.out.print(city + " ");
        }
        System.out.println("\n");
    }

    /**
     * Get the cost for this chromosome. This is the amount of distance that
     * must be traveled.
     */
    double getCost() {
        return cost;
    }

    /**
     * @param i The city you want.
     * @return The ith city.
     */
    int getCity(int i) {
        return cityList[i];
    }

    /**
     * Set the order of cities that this chromosome would visit.
     *
     * @param list A list of cities.
     */
    void setCities(int[] list) {
        for (int i = 0; i < cityList.length; i++) {
            cityList[i] = list[i];
        }
    }

    /**
     * Set the index'th city in the city list.
     *
     * @param index The city index to change
     * @param value The city number to place into the index.
     */
    void setCity(int index, int value) {
        cityList[index] = value;
    }

    /**
     * Sort the chromosomes by their cost.
     *
     * @param chromosomes An array of chromosomes to sort.
     * @param num         How much of the chromosome list to sort.
     */
    public static void sortChromosomes(Chromosome chromosomes[], int num) {
        Chromosome ctemp;
        boolean swapped = true;
        while (swapped) {
            swapped = false;
            for (int i = 0; i < num - 1; i++) {
                if (chromosomes[i].getCost() > chromosomes[i + 1].getCost()) {
                    ctemp = chromosomes[i];
                    chromosomes[i] = chromosomes[i + 1];
                    chromosomes[i + 1] = ctemp;
                    swapped = true;
                }
            }
        }
    }

    /**
     * WRITTEN
     * Utility method to check if a chromosome contains duplicate elements - illegal in permutation
     **/
    public boolean hasDuplicates() {
        TreeSet<Integer> found = new TreeSet<>();
        for (int city: this.cityList) {
            if (found.contains(city)) {
                System.out.println("Duplicate found: " + city);
                return true;
            } else {
                found.add(city);
            }
        }
        return false;
    }
}
