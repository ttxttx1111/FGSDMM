package parallel;

import java.util.Random;

/**
 * Created by 分心 on 2017/6/10.
 */
public class Sampling extends Thread{
    //documents
    //z_i[d]
    // m_k
    //N_kt
    //N_k
    //useIndex

    //pull every iteration
    int[] m_k;
    int[][] N_kt;
    int[] N_k;

    //push every iteration
    int[] m_k_change;
    int[][] N_kt_change;
    int[] N_k_change;

    //initial parameter
    int K;
    int V;
    double alpha;
    double beta;
    int D;
    double alpha0;
    double beta0;
    double[] numerator_index_num;
    int[] numerator_index_overflow;
    double[] denominator_index_num;
    int[] denominator_index_overflow;

    //threads' parameter
    int[] z_i;
    int D_sub;
    Document[] documents;


    double smallDouble = 1e-100;
    double largeDouble = 1e100;
    boolean useIndex;
    Random random;

    public void run(){
            int[] temp1 = N_k.clone();
            int[] temp2 = m_k.clone();
            int[][] temp3 = new int[K][V];

            for(int k = 0;k < K;k++){
				temp3[k] = N_kt[k].clone();
			}

			N_k = temp1;
            m_k = temp2;
            N_kt = temp3;

            for(int d = 0; d < D_sub; d++){
                Document document = documents[d];
                int cluster = z_i[d];
                m_k[cluster]--;
                m_k_change[cluster]--;

                for(int w = 0; w < document.wordNum; w++){
                    int wordNo = document.wordIdArray[w];
                    int wordFre = document.wordFreArray[w];
                    N_kt[cluster][wordNo] -= wordFre;
                    N_k[cluster] -= wordFre;

                    N_kt_change[cluster][wordNo] -= wordFre;
                    N_k_change[cluster] -= wordFre;
                }

                if (useIndex){
                    cluster = sampleCluster_useIndex(document);

                }else{
                    cluster = sampleCluster(document);
                }

                z_i[d] = cluster;
                m_k[cluster]++;
                m_k_change[cluster]++;
                for(int w = 0; w < document.wordNum; w++){
                    int wordNo = document.wordIdArray[w];
                    int wordFre = document.wordFreArray[w];
                    N_kt[cluster][wordNo] += wordFre;
                    N_k[cluster] += wordFre;

                    N_kt_change[cluster][wordNo] += wordFre;
                    N_k_change[cluster] += wordFre;
                }
            }
//			Java_MI.compute_normalized_mutual_information(z_i, labels);
    }

    private int sampleCluster_useIndex(Document document){
        double[] prob = new double[K];
        int[] overflowCount = new int[K];

        for(int k = 0; k < K;k++){
            prob[k] = (m_k[k] + alpha) / (D - 1 + alpha0);

            int index1 = N_k[k];
            int index2 = index1 + document.num;
            double denominator_num = denominator_index_num[index2] / denominator_index_num[index1];
            int denominator_overflow = denominator_index_overflow[index2] - denominator_index_overflow[index1];

            double numerator_num = 1.0;
            int numerator_overflow = 0;
            for(int w = 0;w < document.wordNum;w++){
                int wordNo = document.wordIdArray[w];
                int wordFre = document.wordFreArray[w];
                index1 = N_kt[k][wordNo] ;
                index2 = index1 + wordFre;
                numerator_num *= numerator_index_num[index2] / numerator_index_num[index1];
                numerator_overflow += numerator_index_overflow[index2] - numerator_index_overflow[index1];

                if(numerator_num > largeDouble){
                    numerator_num /= largeDouble;
                    numerator_overflow++;
                }
                if(numerator_num < smallDouble){
                    numerator_num *= largeDouble;
                    numerator_overflow--;
                }
            }

            prob[k] *= numerator_num/denominator_num;
            overflowCount[k] = numerator_overflow - denominator_overflow;
        }

        reComputeProbs(prob, overflowCount, K);
        return chooseK(prob);
    }

    private int sampleCluster(Document document)
    {
        double[] prob = new double[K];
        int[] overflowCount = new int[K];

        for(int k = 0; k < K; k++){
            prob[k] = (m_k[k] + alpha) / (D - 1 + alpha0);
            double valueOfRule2 = 1.0;
            int i = 0;
            for(int w=0; w < document.wordNum; w++){
                int wordNo = document.wordIdArray[w];
                int wordFre = document.wordFreArray[w];
                for(int j = 0; j < wordFre; j++){
                    if(valueOfRule2 < smallDouble){
                        overflowCount[k]--;
                        valueOfRule2 *= largeDouble;
                    }
                    valueOfRule2 *= (N_kt[k][wordNo]  + beta + j) / (N_k[k]  + beta0 + i);
                    i++;
                }
            }
            prob[k] *= valueOfRule2;
        }

        reComputeProbs(prob, overflowCount, K);
        return chooseK(prob);
    }

    private void reComputeProbs(double[] prob, int[] overflowCount, int K)
    {
        int max = Integer.MIN_VALUE;
        for(int k = 0; k < K; k++){
            if(overflowCount[k] > max && prob[k] > 0){
                max = overflowCount[k];
            }
        }

        for(int k = 0; k < K; k++){
            if(prob[k] > 0){
                prob[k] = prob[k] * Math.pow(largeDouble, overflowCount[k] - max);
            }
        }
    }

    private int chooseK(double prob[]){
        for(int k = 1; k < K; k++){
            prob[k] += prob[k - 1];
        }
        double thred = random.nextDouble() * prob[K - 1];
        int kChoosed;
        for(kChoosed = 0; kChoosed < K; kChoosed++){
            if(thred < prob[kChoosed]){
                break;
            }
        }
        return  kChoosed;
    }
}
