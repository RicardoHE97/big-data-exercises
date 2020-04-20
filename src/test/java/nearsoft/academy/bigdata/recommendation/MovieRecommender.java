package nearsoft.academy.bigdata.recommendation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;


public class MovieRecommender {
  private String path;
  private int totalreviews;
  private BiMap<String,Integer> productHash;
  private BiMap<String,Integer> usersHash;

  public MovieRecommender(String path)throws IOException{
    this.totalreviews = 0;
    this.productHash = HashBiMap.create();
    this.usersHash = HashBiMap.create();
    readReviewsFile(path);
  }

  private void readReviewsFile(String path) throws IOException{
    File result = new File("reviews.csv");
    FileWriter fw = new FileWriter(result);
    int current_user = 0;
    int current_product = 0;

    InputStream stream = new GZIPInputStream(new FileInputStream(path));
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    String line;

    while((line = br.readLine()) != null){
      if(line.startsWith("product/productId:")){
        String productId = line.split(" ")[1];
        if(!this.productHash.containsKey(productId)){
          this.productHash.put(productId, this.productHash.size()+1);
          current_product = this.productHash.size();
        }else{
          current_product = this.productHash.get(productId);
        }
        continue;
      }
      if(line.startsWith("review/userId")){
        String userId = line.split(" ")[1];
        if(!this.usersHash.containsKey(userId)){
          this.usersHash.put(userId,this.usersHash.size()+1);
          current_user = this.usersHash.size();
        }else{
          current_user = this.usersHash.get(userId);
        }
      }
      if(line.startsWith("review/score")){
        String score = line.split(" ")[1];
        fw.write(current_user+","+current_product+","+score+"\n");
        this.totalreviews++;
      }
    }
    fw.close();
    br.close();
  }

  public int getTotalProducts() {
    return this.productHash.size();
  }

  public int getTotalUsers() {
    return this.usersHash.size();
  }

  public int getTotalReviews() {
    return this.totalreviews;
  }


  public List<String> getRecommendationsForUser(String user) throws IOException,TasteException {
    DataModel model = new FileDataModel(new File("reviews.csv"));
    UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
    UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
    UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

    int user_id = this.usersHash.get(user);
    List<RecommendedItem> recommendations = recommender.recommend(user_id,3);
    List<String> recommendationsList = new ArrayList<String>();
    BiMap<Integer, String> productHashInverse = this.productHash.inverse();

    for(RecommendedItem recommendation: recommendations){
      recommendationsList.add(productHashInverse.get((int)recommendation.getItemID()));
    }
      return recommendationsList;
  }
}
