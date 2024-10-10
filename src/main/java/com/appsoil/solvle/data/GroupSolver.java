package com.appsoil.solvle.data;

import java.util.*;

public class GroupSolver {
    private Set<String> items;
    private int groupSize;
    private List<Set<String>> constraints = new ArrayList<>();
    private List<Integer> numCorrect = new ArrayList<>();

    public void init(Set<String> items, int groups) {
        this.items = items;
        this.groupSize = items.size() / groups;
    }

    public void addInfo(Set<String> items, int numCorrect) {
        constraints.add(items);
        this.numCorrect.add(numCorrect);
    }

    public List<List<Set<String>>> getPossibleSolutions() {
        List<List<Set<String>>> allSolutions = new ArrayList<>();
        generateCombinations(new ArrayList<>(items), new HashSet<>(), allSolutions, 0);

        return filterSolutions(allSolutions);
    }

    private void generateCombinations(List<String> items, Set<String> current, List<List<Set<String>>> solutions, int start) {
        if (current.size() == groupSize) {
            Set<String> newGroup = new HashSet<>(current);
            List<String> remainingItems = new ArrayList<>(items);
            remainingItems.removeAll(newGroup);
            if (remainingItems.size() == groupSize) {
                List<Set<String>> solution = new ArrayList<>();
                solution.add(newGroup);
                solution.add(new HashSet<>(remainingItems));
                solutions.add(solution);
            }
            return;
        }

        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            generateCombinations(items, current, solutions, i + 1);
            current.remove(items.get(i));
        }
    }

    private List<List<Set<String>>> filterSolutions(List<List<Set<String>>> solutions) {
        List<List<Set<String>>> validSolutions = new ArrayList<>();
        for (List<Set<String>> solution : solutions) {
            if (isValidSolution(solution)) {
                validSolutions.add(solution);
            }
        }
        return validSolutions;
    }

    private boolean isValidSolution(List<Set<String>> solution) {
        for (int i = 0; i < constraints.size(); i++) {
            Set<String> constraint = constraints.get(i);
            int count = numCorrect.get(i);
            int found = 0;

            for (Set<String> group : solution) {
                Set<String> temp = new HashSet<>(constraint);
                temp.retainAll(group);
                found += temp.size();
                if (temp.size() == count) {
                    break;
                }
            }
            if (found != count) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        Set<String> items = new HashSet<>(Arrays.asList("SLING", "BAR", "PUNCH", "LINE", "PIE", "SOUR", "FIZZ", "BUBBLE"));
        GroupSolver splitter = new GroupSolver();
        splitter.init(items, 2);
        splitter.addInfo(new HashSet<>(Arrays.asList("BAR", "PUNCH", "PIE", "SOUR")), 2);
        splitter.addInfo(new HashSet<>(Arrays.asList("PUNCH", "BUBBLE", "FIZZ", "SOUR")), 3);
        splitter.addInfo(new HashSet<>(Arrays.asList("PUNCH", "BUBBLE", "FIZZ", "SLING")), 3);


        List<List<Set<String>>> solutions = splitter.getPossibleSolutions();
        for (List<Set<String>> solution : solutions) {
            System.out.println(solution);
        }
    }
}
