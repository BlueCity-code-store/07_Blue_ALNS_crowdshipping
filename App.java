
public class App {
	public static void main(String[] args) {
		String instance;
		String [] inst_class = {"c","r","rc","C1_2_","R1_2_","RC1_2_"};
		for(int i = 0; i < 1; i++) {
			for(int j = 7; j <= 9; j++) {
				if(i>5){
					System.out.println("Instance exeedes list of instances :( ");
					break;
				}else {
					instance = inst_class[i];
				}
				if(i<3) {
					instance = instance + 10 + j;
				}else {
					instance = instance + j;
				}
				//plot = new Draw(instance,3100,3100,cus);
				System.out.println(instance);
				System.out.println(" Iter " + " Solution " + " Vehi " + " CD " + " Time ");
				
				
				Goldensection PPP = new Goldensection(2,5,70);

				double a = 0; // Lower bound of the search interval
        		double b = 40; // Upper bound of the search interval

        		double min = PPP.goldenSectionSearch(a, b);

        		System.out.println("Approximate minimum point: " + min);
        		System.out.println("Function value at minimum point: " + PPP.getf(min));
				for(int k = 0; k < 1; k++) {
					double time = (double)System.currentTimeMillis()/1000.0;
					ALNS problem = new ALNS(instance);
					problem.initiate(0);
					//problem.print_routes();
					time = (double)System.currentTimeMillis()/1000.0 - time;
					System.out.println(k + " " + problem.incumbent.get_solution() + " " + problem.incumbent.get_vehicles() + " " +problem.incumbent.get_cd() +" " + time);
				}
			}
		}	
	}
}