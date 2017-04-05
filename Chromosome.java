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
        calculateCost(cities);
        double cost1 = getCost();
        for (int i = 0 ;  i < cityList.length; i++) copyCityList[i] = cityList[i];

        int r = generator.nextInt(cityList.length);
        for(int i = r; i < cityList.length; i++) {
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
            cityList[closest] = cityList[i];
            cityList[i] = temp;
        }

        calculateCost(cities);
        if (this.getCost() > cost1) {
            cityList = copyCityList;
            calculateCost(cities);
        }
    }

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

    public Chromosome optBreed(City[] cities, Chromosome mate) {
        return null;
    }

    public Chromosome mutate(City[] cities) {
        int length = cityList.length;
        boolean good = false;
        int a = 0, b = 0;
        while (a == b) {
            a = r.nextInt(length);
            b = r.nextInt(length);
        }
        if (b < a) {
            int temp = b;
            b = a;
            a = temp;
        }
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
        /*
        for (int i = 0 ; i < length; i++) {
            System.out.print("(" + getCity(i) + ", ");
            System.out.print(newList[i] + ")");
            System.out.print(" ");
        }
        System.out.println();
        */
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
        /*
        System.out.println("Range: " + x[0] + ", " + x[1]);
        System.out.println("Count: " + count);
        */
        /*
        boolean ok = false;
        for (int i = 0; i < newList.length; i++) {
            if (newList[i] != getCity(i)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("Origional and PMX are the same!");
        }
        ok = false;
        for (int i = 0; i < newList.length; i++) {
            if (newList[i] != mate.getCity(i)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("Mate and PMX are the same!");
        }
        */
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
