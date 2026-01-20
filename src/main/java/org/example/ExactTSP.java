package org.example;

import java.util.*;

public class ExactTSP {

    private static int n;
    private static double[][] dist;
    private static double bestLength = Double.MAX_VALUE;
    private static List<Integer> bestTour = new ArrayList<>();

    public static void main(String[] args) {
        // Example of a small graph (n = 8–10 for fast computation)
        n = 8;
        dist = new double[n][n];
        Random rand = new Random();

        // Generate random distances from 1 to 40
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = rand.nextInt(40) + 1;
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }

        // Initial greedy tour to estimate upper bound
        double greedy = calculateGreedy();
        System.out.println("Greedy estimate: " + greedy);

        // Run exact search
        long startTime = System.currentTimeMillis();

        List<Integer> startTour = new ArrayList<>();
        boolean[] visited = new boolean[n];

        startTour.add(0);
        visited[0] = true;

        solveExact(1, startTour, visited, 0);

        long endTime = System.currentTimeMillis();

        System.out.println("\nOptimal tour:");
        System.out.println(bestTour);
        System.out.println("Optimal length: " + bestLength);
        System.out.println("Execution time: " + (endTime - startTime) / 1000.0 + " seconds");
    }

    // Greedy algorithm for initial upper bound
    private static double calculateGreedy() {
        boolean[] visited = new boolean[n];
        List<Integer> tour = new ArrayList<>();
        tour.add(0);
        visited[0] = true;
        double total = 0;

        for (int i = 1; i < n; i++) {
            int curr = tour.get(tour.size() - 1);
            double min = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < n; j++) {
                if (!visited[j] && dist[curr][j] < min) {
                    min = dist[curr][j];
                    next = j;
                }
            }
            tour.add(next);
            visited[next] = true;
            total += min;
        }
        total += dist[tour.get(tour.size() - 1)][0];
        return total;
    }

    // Recursive branch-and-bound
    private static void solveExact(int pos, List<Integer> currentTour,
                                   boolean[] visited, double currentLength) {
        if (pos == n) {
            currentLength += dist[currentTour.get(pos - 1)][currentTour.get(0)];
            if (currentLength < bestLength) {
                bestLength = currentLength;
                bestTour = new ArrayList<>(currentTour);
                bestTour.add(currentTour.get(0)); // close the cycle
                System.out.println("Found better solution: " + bestLength);
            }
            return;
        }

        for (int next = 0; next < n; next++) {
            if (!visited[next]) {
                double newLength = currentLength + dist[currentTour.get(pos - 1)][next];
                if (newLength >= bestLength) continue; // branch pruning

                visited[next] = true;
                currentTour.add(next);
                solveExact(pos + 1, currentTour, visited, newLength);
                currentTour.remove(currentTour.size() - 1);
                visited[next] = false;
            }
        }
    }
}
