import java.util.ArrayList;
import java.util.Random;
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
    }

    Chromosome(City[] cities, int[] cityList) {
        this.cityList = cityList;
        calculateCost(cities);
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

    public Chromosome mutate(City[] cities) {
        int length = cityList.length;
        boolean good = false;
        int a = 0, b = 0;
        Random r = new Random();
        while (!good) {
            a = r.nextInt(length);
            b = r.nextInt(length);
            if (a != b) {
                good = true;
            }
        }
        if (b < a) {
            int temp = b;
            b = a;
            a = temp;
        }
        int interval = b - a;
        int[] mutatedCityList = cityList.clone();
        for (int i = 0; i < interval/2; i++) {
            int temp = mutatedCityList[a+i];
            mutatedCityList[a+i] = mutatedCityList[b-i];
            mutatedCityList[b-i] = temp;
        }
        return new Chromosome(cities, mutatedCityList);
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
                return true;
            } else {
                found.add(city);
            }
        }
        return false;
    }
}
