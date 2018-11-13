package parallel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Random;

public class Model{
	int K;					// count of clusters
	double alpha;
	double beta;
	String dataset;
	String ParametersStr;
	int V;					// size of vocabulary
	int D;					// count of documents
	int iterNum;			// num of iteration
	double alpha0;
	double beta0;
	int[] z_i;				// z_i[i] : cluster assignment of ith doc
	int[] m_k;				// m_k[k] : count of documents in kth cluster
	int[][] N_kt;			// N_kt[k][t] : count of word t in kth cluster
	int[] N_k;				// N_k[k] : count of words in kth cluster

	double[] numerator_index_num;
	int[] numerator_index_overflow;
	double[] denominator_index_num;
	int[] denominator_index_overflow;

	int[] labels;
	Random random;

	int thread_num;
	Sampling[] threads;
	int D_sub;			    // (int)Math.ceil((double)D/(double)thread_num)

	ArrayList<Document[]> documentList;
	ArrayList<int[]> zList;
	double communication_time ;
	double computeTime;



	public Model(int K, int V, int iterNum, double alpha, double beta,
			String dataset, String ParametersStr)
	{
		this.dataset = dataset;
		this.ParametersStr = ParametersStr;
		this.alpha = alpha;
		this.beta = beta;
		this.K = K;
		this.V = V;
		this.iterNum = iterNum;
		this.alpha0 = K * alpha;
		this.beta0 = V * beta;

		this.m_k = new int[K];
		this.N_k = new int[K];
		this.N_kt = new int[K][V];
		for(int k = 0; k < K; k++){
			this.N_k[k] = 0;
			this.m_k[k] = 0;
			for(int t = 0; t < V; t++){
				this.N_kt[k][t] = 0;
			}
		}
	}

	public void intialize(DocumentSet documentSet) throws Exception {

		random = new Random();
		D = documentSet.D;
		z_i = new int[D];
		for(int d = 0; d < D; d++){
			Document document = documentSet.documents.get(d);
			int cluster = (int) (K * random.nextDouble());
			z_i[d] = cluster;
			m_k[cluster]++;
			for(int w = 0; w < document.wordNum; w++){
				int wordNo = document.wordIdArray[w];
				int wordFre = document.wordFreArray[w];
				N_kt[cluster][wordNo] += wordFre;
				N_k[cluster] += wordFre;
			}
		}



//		labels = Main.load_labels("../data/" + dataset);
	}

	public void setMultiThread(boolean useIndex) throws Exception {
		threads = new Sampling[thread_num];
		for(int i = 0;i < thread_num;i++){
			threads[i] = new Sampling();
			threads[i].K = K;
			threads[i].alpha = alpha;
			threads[i].alpha0 = alpha0;
			threads[i].beta = beta;
			threads[i].beta0 = beta0;
			threads[i].denominator_index_num = denominator_index_num;
			threads[i].denominator_index_overflow = denominator_index_overflow;
			threads[i].numerator_index_num = numerator_index_num;
			threads[i].numerator_index_overflow = numerator_index_overflow;
			threads[i].documents = documentList.get(i);
			threads[i].D_sub = threads[i].documents.length;
			threads[i].V = V;

			threads[i].z_i = zList.get(i);
			threads[i].useIndex = useIndex;
			threads[i].random = new Random();

			threads[i].N_k = N_k;
			threads[i].N_kt = N_kt;
			threads[i].m_k = m_k;
//			threads[i].N_k = new int[K];
//			threads[i].N_kt = new int[K][V];
//			threads[i].m_k = new int[K];
//
//			for(int k = 0;k < K;k++){
//				threads[i].N_k[k] = N_k[k];
//				threads[i].m_k[k] = m_k[k];
//				for(int v = 0;v < V;v++){
//					threads[i].N_kt[k][v] = N_kt[k][v];
//				}
//			}
			threads[i].N_k_change = new int[K];
			threads[i].N_kt_change = new int[K][V];
			threads[i].m_k_change = new int[K];

//			for(int k = 0;k < K;k++){
//				threads[i].N_k[k] = N_k[k];
//				threads[i].m_k[k] = m_k[k];
//				for(int v = 0;v < V;v++){
//					threads[i].N_kt[k][v] = N_kt[k][v];
//				}
//			}
//			threads[i].N_k_change = new int[K];
//			threads[i].N_kt_change = new int[K][V];
//			threads[i].m_k_change = new int[K];
		}
	}

	public void setData(DocumentSet documentSet,int thread_num){
		int D_sub = (int)Math.ceil((double)D/(double)thread_num);

		this.thread_num = thread_num;
		documentList = new ArrayList<>();
		zList = new ArrayList<>();
		ArrayList<Document> d = documentSet.documents;
		for(int t = 0;t < thread_num;t++){
			int num = (t == thread_num - 1 ? D - D_sub * t : D_sub);
			Document[] documents = new Document[num];
			int[] z  = new int[num];
			int count = 0;
			for(int j = t * D_sub;j < t * D_sub + num;j++){
				documents[count] = d.get(j);
				z[count] = z_i[j];
				count++;
			}
			documentList.add(documents);
			zList.add(z);
		}




	}

	public void gibbsSampling(boolean useIndex) throws Exception {
//		for(int j = 0;j < thread_num;j++){
//
//			threads[j].N_k = new int[K];
//			threads[j].N_kt = new int[K][V];
//			threads[j].m_k = new int[K];
//		}
		 communication_time = 0;
		double start = 0;
		double end1 = 0;
		double end2 = 0;
		double end3 = 0;

		for(int i = 0; i < iterNum; i++){
			start = System.currentTimeMillis();
			setMultiThread(useIndex);
			end1 = System.currentTimeMillis();

			for(int j = 0;j < thread_num;j++){
				threads[j].start();
			}

			for(Sampling thread:threads){
				thread.join();
			}
			end2 = System.currentTimeMillis();

			for(int j = 0;j < thread_num;j++){
				int[] N_k_change = threads[j].N_k_change;
				int[] m_k_change = threads[j].m_k_change;
				int[][] N_kt_change = threads[j].N_kt_change;
				for(int k = 0;k < K;k++){
					m_k[k] += m_k_change[k];
					N_k[k] += N_k_change[k];
					for(int v = 0;v < V;v++){
						N_kt[k][v] += N_kt_change[k][v];
					}
				}
			}
			end3 = System.currentTimeMillis();
			communication_time += end1 - start + end3 - end2;
			computeTime += end2 - end1;
//			Java_MI.compute_normalized_mutual_information(z_i, labels);

		}
		mergeZ();

	}

	private void mergeZ(){
		int temp = 0;
		for(int i = 0;i < thread_num;i++){
			for(int j = 0;j < threads[i].D_sub;j++){
				z_i[j + temp] = threads[i].z_i[j];
			}
			temp += threads[i].D_sub;
		}
	}


	public void output(parallel.DocumentSet documentSet, String outputPath) throws Exception
	{
		String outputDir = outputPath + dataset + ParametersStr + "/";

		File file = new File(outputDir);
		if(!file.exists()){
			if(!file.mkdirs()){
				System.out.println("Failed to create directory:" + outputDir);
			}
		}

		outputClusteringResult(outputDir, documentSet);
	}

	public void outputClusteringResult(String outputDir, parallel.DocumentSet documentSet) throws Exception
	{
		String outputPath = outputDir + dataset + "ClusteringResult.txt";
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter
				(new FileOutputStream(outputPath), "UTF-8"));
		for(int d = 0; d < documentSet.D; d++){
			int topic = z_i[d];
			writer.write(topic + "\n");
		}
		writer.flush();
		writer.close();
	}




}
