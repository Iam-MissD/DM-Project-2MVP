package ch.zhaw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class App {
    public static void main(String[] args) {

        // Disable Logger-Messages
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.mongodb.driver").setLevel(Level.OFF);

        // Initialize the Scanner for User-Input
        Scanner scanner = new Scanner(System.in);

        // Define the Connection to MongoDB
        ConnectionString connectionSTring = new ConnectionString("");   // <------- CONNECTION-STRING FOR MONGODB
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionSTring)
                .serverApi(ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build())
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("Project2MVP");

        // Collections in the MongoDB
        MongoCollection<org.bson.Document> food = database.getCollection("foods");
        MongoCollection<org.bson.Document> users = database.getCollection("users");



        ///////////////////////////////////////////////////////////////////////////
        // START OF THE GAME
        ///////////////////////////////////////////////////////////////////////////


        int totalPoints = 0;

        System.out.println("Welcome to the Food Quiz!");

        totalPoints += runQuiz("calories", scanner, food);
        System.out.println("\n");
        totalPoints += runQuiz("total_fat", scanner, food);
        System.out.println("\n");
        totalPoints += runQuiz("protein", scanner, food);
        System.out.println("\n");
        totalPoints += runQuiz("fiber", scanner, food);
        System.out.println("\n");
        totalPoints += runQuiz("sugars", scanner, food);
        System.out.println("\n");

        System.out.println("Your total points are: " +totalPoints);

        System.out.println("\nPlease enter your name: ");
        String name = scanner.nextLine();

        addPointsToDB(name, totalPoints, users);

        String leaderboard = getLeaderboard(users);

        System.out.println("\n" + leaderboard);
    }



    ///////////////////////////////////////////////////////////////////////////
    // RUN THE QUIZ
    ///////////////////////////////////////////////////////////////////////////

    // Method to run the quiz for a specific nutritionType and return the achieved
    // points
    public static int runQuiz(String nutritionType, Scanner scanner,
            MongoCollection<org.bson.Document> foodCollection) {
        ArrayList<QuizEntry> quizList = question(nutritionType, foodCollection);
        ArrayList<Integer> answerSequence = getAnswer(scanner);
        return checkAnswer(nutritionType, quizList, answerSequence);
    }



    ///////////////////////////////////////////////////////////////////////////
    // PARTS OF THE QUIZ
    ///////////////////////////////////////////////////////////////////////////

    // Method to create the question for a specific nutritionType
    public static ArrayList<QuizEntry> question(String nutritionType,
            MongoCollection<org.bson.Document> foodCollection) {

        Stats stats = getStats(nutritionType, foodCollection);
        System.out.println("\nQuestions about " + nutritionType + ":\n(Maximum: " + stats.getMaximum() + ", Average: "
                + stats.getAverage() + ")");

        ArrayList<QuizEntry> quizList = getQuizList(nutritionType, stats.getAverage(), foodCollection);

        System.out.println("Please order the following foods by " + nutritionType + " in ascending order: ");
        System.out.println("Only use 1, 2, 3; separate them by comma.\n");
        
        for (int i = 0; i < quizList.size(); i++) {
            quizList.get(i).setPosition(i + 1);
            System.out.println(
                    quizList.get(i).getPosition() + ") " + quizList.get(i).getFoodname() + quizList.get(i).getValue());
        }
        
        return quizList;
    }


    // Method to get the User-Answer
    public static ArrayList<Integer> getAnswer(Scanner scanner) {
        String answer = scanner.nextLine();
        String[] list = answer.split(",");

        ArrayList<Integer> answerSequence = new ArrayList<>();
        for (String element : list) {
            answerSequence.add(Integer.parseInt(element));
        }
        return answerSequence;
    }


    // Method to check answer and return points
    public static int checkAnswer(String nutritionType, ArrayList<QuizEntry> quizList,
            ArrayList<Integer> answerSequence) {

        int points = 0;
        ArrayList<QuizEntry> answerList = new ArrayList<>();
        ArrayList<QuizEntry> sortedList = new ArrayList<>();

        // Create a new list called sortedList as a copy of the quizList
        for (QuizEntry quizEntry : quizList) {
            sortedList.add(quizEntry);
        }

        // Sort the sortedList by value
        sortedList = sortedList.stream()
                .sorted((x, y) -> Double.compare(x.getValue(), y.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));

        // Create the AnswerList
        for (int i = 0; i < answerSequence.size(); i++) {
            answerList.add(quizList.get(answerSequence.get(i) - 1));
        }

        // Checks the order of the answerList and the sortedList and gives points for
        // every correct position
        for (int i = 0; i < answerList.size(); i++) {
            if (answerList.get(i).getFoodname().equals(sortedList.get(i).getFoodname())) {
                points++;
            }
        }

        // In case you use the same number twice, you only get one point
        if (points == 2) {
            points = 1;
        }

        // Maximum Points are 2, so in case you have three correct you get 2 instead of
        // 3
        if (points == 3) {
            points = 2;
        }

        // Prints the statements for the achieved points
        if (points == 0) {
            System.out.println("\nToo bad, you didn't get any points for the " + nutritionType + " section.");
        } else if (points == 1) {
            System.out.println(
                    "\nYour answer is partially correct! You get 1 point for the " + nutritionType + " section.");
        } else {
            System.out.println("\nYour answer is  correct! You get 2 point for the " + nutritionType + " section.");
        }

        // Prints the correct order
        System.out.println("\nThe correct order for " + nutritionType + " per 100g is as followed:");
        for (QuizEntry food : sortedList) {
            System.out
                    .println(food.getPosition() + ") " + food.getValue() + "g "
                            + nutritionType);
        }

        return points;
    }



    ///////////////////////////////////////////////////////////////////////////
    // MONGODB-ENTRY
    ///////////////////////////////////////////////////////////////////////////

    // Adds a new Result to the DataBase
    public static void addPointsToDB(String name, int points, MongoCollection<org.bson.Document> leaderboard) {

        Result result = new Result(name, points);

        Gson gson = new GsonBuilder().create(); // initiate gson-builder
        String resultJson = gson.toJson(result); // object to json
        Document resultDoc = Document.parse(resultJson); // json to bson
        leaderboard.insertOne(resultDoc);

    }



    ///////////////////////////////////////////////////////////////////////////
    // MONGODB-AGGREGATIONS
    ///////////////////////////////////////////////////////////////////////////

    public static String getLeaderboard(MongoCollection<org.bson.Document> users) {
        String leaderboard = "Leaderboard:\n";

        ArrayList<Document> list = users.aggregate(
            Arrays.asList(new Document("$sort", 
            new Document("points", -1L)), 
            new Document("$limit", 3L)))
            .into(new ArrayList<Document>());


            for (int i=0; i<list.size(); i++){
                leaderboard += (i+1) +") " +list.get(i).getString("name") +" - Points: " +list.get(i).getInteger("points") +"\n";
            }

        return leaderboard;

    }


    // Method to get the Stats for all the Quiz-Parts
    public static Stats getStats(String nutritionType, MongoCollection<org.bson.Document> foodCollection) {

        // First group all the foods and aggregate the maximum and average of the chosen
        // nutritionType
        // Project the maximum and average, to round to average to only 2 digits and
        // convert them to Double
        // The conversion is necessary because not all nutritionTypes are doubles
        ArrayList<Document> list = foodCollection.aggregate(
                Arrays.asList(new Document("$group",
                        new Document("_id", "Summary")
                                .append("maximum",
                                        new Document("$max", "$" + nutritionType))
                                .append("average",
                                        new Document("$avg", "$" + nutritionType))),
                        new Document("$project",
                                new Document("maximum",
                                        new Document("$toDouble", "$maximum"))
                                        .append("average",
                                                new Document("$toDouble",
                                                        new Document("$round", Arrays.asList("$average", 1L)))))))
                .into(new ArrayList<Document>());

        Stats stats = new Stats(nutritionType, list.get(0).getDouble("maximum"), list.get(0).getDouble("average"));

        return stats;
    }


    // Method to get a sample of three Foods; 2 above Average, 1 below Average
    public static ArrayList<QuizEntry> getQuizList(String nutritionType, Double average,
            MongoCollection<org.bson.Document> foodCollection) {

        // First group all the foods and aggregate the maximum and average of the chosen
        // nutritionType
        // Project the maximum and average, to round to average to only 2 digits and
        // convert them to Double
        // The conversion is necessary because not all nutritionTypes are doubles
        ArrayList<Document> list = foodCollection.aggregate(
                Arrays.asList(new Document("$facet",
                        new Document("greaterThanAvg", Arrays.asList(new Document("$match",
                                new Document(nutritionType,
                                        new Document("$gt", average))),
                                new Document("$project",
                                        new Document("value",
                                                new Document("$toDouble", "$" + nutritionType))
                                                .append("name", "$name")),
                                new Document("$sample",
                                        new Document("size", 2L))))
                                .append("smallerThanAvg", Arrays.asList(new Document("$match",
                                        new Document(nutritionType,
                                                new Document("$lt", average))),
                                        new Document("$project",
                                                new Document("value",
                                                        new Document("$toDouble", "$" + nutritionType))
                                                        .append("name", "$name")),
                                        new Document("$sample",
                                                new Document("size", 1L))))),
                        new Document("$project",
                                new Document("quizList",
                                        new Document("$concatArrays",
                                                Arrays.asList("$greaterThanAvg", "$smallerThanAvg")))),
                        new Document("$unwind", "$quizList"),
                        new Document("$replaceRoot",
                                new Document("newRoot", "$quizList"))))
                .into(new ArrayList<Document>());

        ArrayList<QuizEntry> quizList = new ArrayList<>();

        for (Document d : list) {
            quizList.add(new QuizEntry(0, d.getString("name"), d.getDouble("value")));
        }

        Collections.shuffle(quizList);
        return quizList;
    }
}
