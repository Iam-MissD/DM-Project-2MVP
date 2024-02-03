package ch.zhaw;

public class Stats {
    private String nutritionType;
    private Double maximum;
    private Double average;

    public Stats(String nutritionType, Double maximum, Double average) {
        this.nutritionType = nutritionType;
        this.maximum = maximum;
        this.average = average;
    }

    public String getNutritionType() {
        return nutritionType;
    }
    public Double getMaximum() {
        return maximum;
    }
    public Double getAverage() {
        return average;
    }



    
}


