package ruteo.distanceFetcher;

import ruteo.UnknownValueException;

class Matrix{
    private double[][] times;
    private double[][] distances;
    private double[][] weights;

    Matrix(String matrixKind, double[][] matrix) throws UnknownValueException {
        switch (matrixKind) {
            case "times":
                this.times = matrix;
                break;
            case "distances":
                this.distances = matrix;
                break;
            case "weights":
                this.weights = matrix;
                break;
            default:
                throw new UnknownValueException("Unknown matrix kind");
        }
    }

    Matrix(double[][] times, double[][] distances){

        this.times = times;
        this.distances = distances;
    }

    Matrix(double[][] times, double[][] distances, double[][] weights){
        this.times = times;
        this.distances = distances;
        this.weights = weights;
    }

    double[][] getTimes(){
        return times;
    }
    double[][] getDistances(){
        return distances;
    }
    double[][] getWeights(){
        return weights;
    }

    String timesToString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (double[] time : this.times) {
            for (double aTime : time) {
                stringBuilder.append(aTime).append(" ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
    String distanceToString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (double[] distances : this.distances) {
            for (double aDistances : distances) {
                stringBuilder.append(aDistances).append(" ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}