package ch.zhaw;


// This class is to generalize the different parts of the quiz
// Every quiz-part shows foodname and value
// The position is needed to remember at what position the three foods are placed in the question
// The position has to be set in the beginning (value and foodname don't need setters)
public class QuizEntry {
    

    private int position;
    private String foodname;
    private double value;


    public QuizEntry(int position, String foodname, double value) {
        this.position = position;
        this.foodname = foodname;
        this.value = value;
    }


    public int getPosition() {
        return position;
    }
    public String getFoodname() {
        return foodname;
    }
    public double getValue() {
        return value;
    }

    public void setPosition(int position){
        this.position = position;
    }

}
