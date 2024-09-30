import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



public class ALNS {
	public int num_customers = Parameters.num_customers;
	int Q = 200;
	int Q2 = 100;
	public Depot depot_start;
	public Solution incumbent;
	public double incumbent_value;
	public List<Route> incumbent_solution = new ArrayList<>();
	public Map<Integer,Cluster> all_clusters = new HashMap<Integer,Cluster>();
	public Map<Integer,Customer> all_customers = new HashMap<Integer,Customer>();
	public Map<Integer,Node> all_nodes = new HashMap<Integer,Node>();
	public List<Route> all_routes = new ArrayList<>();
	public double lower_bound = Double.MAX_VALUE;
	private String instance;
	private double total_time;
	public double [] Demand_Array;
	public double [] Time_e_Array;
	public double [] Time_l_Array;
	public double [] Time_Service;
	
	public double [][] Cost;
	
	public int [][] Nearest;
	public double v_cost(int pre) {
		return Parameters.recourse_Vcost_1[pre];
	}
	public double f_cost(int pre) {
		return Parameters.recourse_Fcost_1[pre];
	}
	public ALNS(String s) {
		total_time = (double)System.currentTimeMillis()/1000.0;
		instance = s;
		ReadData();
		fill_cost();
		fill_nearest();
		fill_demand_array();
		create_clusters();
		Parameters.Cal_CD_max();
		C_W w = new C_W();
		incumbent = w.begin();
		//double cw_time = (double)System.currentTimeMillis()/1000.0 - total_time;
		incumbent.update_vehicle(1);
		
		//System.out.println("Routes " + incumbent.get_vehicles() + " cw time == " + cw_time);
		//System.out.println("Cost = " + incumbent.get_solution());
		//for(int r = 0; r < incumbent.routes_0.size(); r++) {
		//	if(incumbent.routes_0.get(r).feasible()) {
		//		System.out.print("Feasible  load = " + incumbent.routes_0.get(r).load + " ");
		//	}else { System.out.print("Not Feasible  ");}
		//	Point p = incumbent.routes_0.get(r).start;
		//	System.out.print("Depot --> ");
		//	while(p != null) {
		//		System.out.print(", " + p.id);
		//		p = p.next;
		//	}
		//	System.out.println(",");
		//}
	}
	private class Cluster{
		public int id;
		public double penalty;
		private double route_duration;
		private List<Integer> cluster_customers;
		public Cluster( List<Integer> c) {
			this.id = all_clusters.size();
			this.cluster_customers = new ArrayList<>();
			for(int i = 0; i < c.size(); i++) {
				this.cluster_customers.add(c.get(i));
			}
			all_clusters.put(this.id, this);
			if(this.id == 4){
				this.penalty = 1;
			}else{
				this.penalty = 0.5;
			}
			set_route_duration();
			set_customers_r_d();
		}
		public void set_customers_r_d() {
			int size = cluster_customers.size();
			for(int i = 0; i < size; i ++) {
				all_customers.get(cluster_customers.get(i)).set_r_d(this.route_duration);
				all_customers.get(cluster_customers.get(i)).set_cluster(this.id);
				all_customers.get(cluster_customers.get(i)).set_geo(this.penalty);
			}
		}
		public void set_route_duration () {
			double e = 0.0;
			//-----------------------------------------------------------//
			// -------- Neighborhood centers and route duration -------- //
			//-----------------------------------------------------------//
			double centerX = 0.0;
			double centerY = 0.0;
			int size = cluster_customers.size();
			for(int i = 0; i < size; i ++) {
				centerX += all_customers.get(cluster_customers.get(i)).getxcoord();
				centerY += all_customers.get(cluster_customers.get(i)).getycoord();
			}
			centerX = centerX/(double)(size);
			centerY = centerY/(double)(size);
			e = Math.sqrt(Math.pow(centerX - depot_start.getxcoord(), 2) + Math.pow(centerY - depot_start.getycoord(), 2));
			this.route_duration = e*Parameters.r_duration  + Parameters.r_duration_v;
			//System.out.println("Cluster " + this.id + " route duration " + route_duration);
		}
	}

	public class ALNS_1{
		Solution solution;
		List<Integer> removed;
		//List<Integer> removed_2;
		Random rand;
		int iterations;
		int curr_it = 0;
		double Temperature;
		boolean heating;
		double cool = 0.99997;
		double heat = 1.001;
		List<Integer> wheel;
		double [] weight;
		public ALNS_1() {
			this.solution = new Solution (incumbent);
			this.removed = new ArrayList<>();
			//this.removed_2 = new ArrayList<>();
			this.rand = new Random();
			this.Temperature = 600.0;
			this.heating = false;
			this.wheel = new ArrayList<>();
			this.weight = new double [num_customers];
			for(int i = 0; i < num_customers; i++) {
				this.wheel.add(i);
				this.weight[i] = 1.0;
			}
		}
		public void update_Temperature() {
			//System.out.println(Temperature);
			Temperature*=cool;
			if(Temperature < 0.003 ) {
				//Temperature+=rand.nextInt(100);
				Temperature = 400;
				System.out.println("Reheating happens " + Temperature);
				//Temperature+=incumbent.s_value*0.80;
			}
		}
		
		public void LNS() {
			Solution sol = new Solution (solution); 
			int q = rand.nextInt((int)Math.ceil(Parameters.num_customers*0.15)) + 5;
			//int q = (int)Math.ceil(Parameters.num_customers*0.25);
			//Random remove q customers;
			double w1 = Math.random();
			if(w1 < 0.7) {
				random_remove(q, sol);
			}else if (w1 < 0.8){
				remove_route(sol);
			}else if (w1 < 0.9) {
				remove_first_customers(sol);
			}else {
				remove_last_customers(sol);
			}
			sol.update_vehicle(1); // sort and price routes.
			double w = Math.random();
			if(w < 0.7) {
				random_insert(sol,1);
			}else if(w<0.8){
				random_insert_with_noise(sol);
			}else if(w<0.9){
				insert_PV(q, sol);
			}else {
				insert_CD(q, sol);
			}
			sol.update_vehicle(1);
			if(incumbent.s_value > sol.get_solution()) {
				incumbent = sol;
				//System.out.println(incumbent.s_value+"--" + Temperature + "--"+ curr_it);
			}
			if(accept(sol)) {
				solution = sol;
			}
			update_Temperature();
		}
		private void random_remove(int q, Solution sol) {
			while(removed.size() < q) {
				int i = wheel.get(rand.nextInt(wheel.size()));
				if (!removed.contains(i)) {
					removed.add(i);
					if(sol.cus[i].next != null) {
						if(sol.cus[i].prev != null) {
							sol.cus[i].next.prev = sol.cus[i].prev;
							sol.cus[i].prev.next = sol.cus[i].next;
							sol.cus[i].next = null;
							sol.cus[i].prev = null;
						}else {
							for(int k = 0; k < sol.routes_0.size(); k ++) {
								if(i == sol.routes_0.get(k).start.id) {
									sol.routes_0.get(k).start = sol.cus[i].next;
									sol.cus[i].next.prev = null;
									sol.cus[i].next = null;
									sol.cus[i].prev = null;
									break;
								}
							}
						}
					}else {
						if(sol.cus[i].prev != null) {
							sol.cus[i].prev.next = null;
							sol.cus[i].prev = null;
						}else {
							for(int k = 0; k < sol.routes_0.size(); k ++) {
								if(i == sol.routes_0.get(k).start.id) {
									sol.routes_0.remove(k);
									break;
								}
							}	
						}
					}	
				}
			}
		}private void remove_route(Solution sol) {
			int k2 = rand.nextInt((int)Math.ceil(sol.routes_0.size()*0.25));
			for(int k1 = 0; k1 < 2 + k2; k1 ++) {
				if(sol.routes_0.size()==1) {
					break;}
				int k = rand.nextInt(sol.routes_0.size());
				int i = sol.routes_0.get(k).start.id;
				removed.add(i);
				Point p = sol.cus[i];
				while(p.next !=null) {
					sol.cus[i].next.prev = null;
					p = sol.cus[i].next;
					sol.cus[i].next = null;
					sol.cus[i].prev = null;
					i = p.id;
					removed.add(i);	
				}
				sol.routes_0.remove(k);
			}
		}
		private void remove_first_customers(Solution sol) {
			for(int k1 = 0; k1 < sol.routes_0.size(); k1 ++) {
				int k = (int)Math.ceil(rand.nextInt(sol.routes_0.get(k1).size)*0.5);
				if(k == sol.routes_0.get(k1).size) {continue;}
				int i = sol.routes_0.get(k1).start.id;
				//System.out.println("k " + k);
				Point p = sol.cus[i];
				int count =0;
				while(p.next !=null && count<k) {
					sol.cus[i].next.prev = null;
					p = sol.cus[i].next;
					sol.routes_0.get(k1).start = sol.cus[p.id];
					sol.cus[i].next = null;
					sol.cus[i].prev = null;
					removed.add(i);
					count +=1;
					i = p.id;
				}
			}
		}
		private void remove_last_customers(Solution sol) {
			for(int k1 = 0; k1 < sol.routes_0.size(); k1 ++) {
				int k = (int)Math.ceil(rand.nextInt(sol.routes_0.get(k1).size)*0.5);
				if(k == sol.routes_0.get(k1).size) {continue;}
				int i = sol.routes_0.get(k1).end.id;
				//System.out.println("k " + k);
				Point p = sol.cus[i];
				int count =0;
				while(p.prev !=null && count<k) {
					sol.cus[i].prev.next = null;
					p = sol.cus[i].prev;
					sol.routes_0.get(k1).end = sol.cus[p.id];
					sol.cus[i].next = null;
					sol.cus[i].prev = null;
					removed.add(i);
					count +=1;
					i = p.id;
				}
			}
		}

		/////////////////////////////////////////////////////////////////
		///////////// insertion /////////////////////////////////////////
		/////////////////////////////////////////////////////////////////
		
		private void random_insert(Solution sol, int method ) {
			while(removed.size() > 0) {
				//insert
				//if(Math.random() < 0.5) {removed_2.add(removed.get(removed.size()-1));}
				Point cus1 = sol.cus[removed.get(removed.size()-1)]; //customer going to be inserted
				removed.remove(removed.size()-1);// remove customer from the list 
				Point ins = null; // best customer where it is going to be inserted
				int route = 0;
				boolean inserted = false; // 
				boolean before = false; // before or after customer
				// boolean regular = true;
				double best_improvement = Double.MAX_VALUE; // best insertion value so far
				for(int k = 0; k < sol.routes_0.size(); k ++) {
					if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[0]) {
						continue;
					}
					int r_type = sol.routes_0.get(k).type;
					boolean CD_to_PV = false;
					if(r_type > 0){
						if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[1]) {
							CD_to_PV = true;
						}
					}
					//double route_duration = all_customers.get(sol.routes1.get(k).last()).get_r_d();
					double time = 0.0;
					//double distance = sol.routes1.get(k).distance;
					Point p = sol.routes_0.get(k).start;
					double cost = insert_cost(p.prev, cus1, p);
					if(method == 0){
						cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					}else{
						cost = dcm_cost(cost,CD_to_PV,all_customers.get(cus1.id).d(),all_customers.get(cus1.id).geo);
					}

					//if(distance + cost > route_duration) {}
					if(cost < best_improvement) {
						//check if feasible 
						if(feasible_insert(cus1, time, p.prev, p)) {
							best_improvement = cost;
							ins = p;
							before = true;
							route = k;
							inserted = true;
						}	
					}
					while(p.next != null) {
						//if(p.prev!= null) {
						//	distance += Cost[p.prev.id][p.id];
						//	}else {distance += Cost[depot_start.id][p.id];}
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						cost = insert_cost(p, cus1, p.next);
						if(method == 0){
							cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
						}else{
							cost = dcm_cost(cost,CD_to_PV,all_customers.get(cus1.id).d(),all_customers.get(cus1.id).geo);
						}
						if(cost < best_improvement) {
							//check if feasible 
							if(feasible_insert(cus1, time, p, p.next)) {
								best_improvement = cost;
								ins = p.next;
								before = true;
								route = k;
								inserted = true;
							}
						}
						p = p.next;
					}

					cost = insert_cost(p, cus1, p.next);
					
					if(method == 0){
						cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					}else{
						cost = dcm_cost(cost,CD_to_PV,all_customers.get(cus1.id).d(),all_customers.get(cus1.id).geo);
					}
					
					if(cost < best_improvement) {
						//check if feasible 
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						if(feasible_insert(cus1, time, p, p.next)) {
							best_improvement = cost;
							ins = p;
							before = false;
							route = k;
							inserted = true;
						}	
					}
				}
				//insert at ins
				if(inserted) {
					
					if(before) {
						
						if(ins != null) {
							if(ins.prev!=null) {
								cus1.next = ins;
								cus1.prev = ins.prev;
								ins.prev.next=cus1;
								ins.prev=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}else {
								ins.prev=cus1;
								cus1.next = ins;
								cus1.prev = null;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								sol.routes_0.get(route).start = cus1;
							}
						}
					}else {
						if(ins != null) {
							if(ins.next!=null) {
								cus1.prev = ins;
								cus1.next = ins.next;
								ins.next.prev=cus1;
								ins.next=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}else {
								ins.next=cus1;
								cus1.next = null;
								cus1.prev = ins;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}
						}	
					}
				}else {
					sol.routes_0.add(new Route(cus1));
				}
			}
			
		}
		private void random_insert_with_noise(Solution sol) {
			while(removed.size() > 0) {
				//insert
				//if(Math.random()<0.5) {removed_2.add(removed.get(removed.size()-1));}
				Point cus1 = sol.cus[removed.get(removed.size()-1)];//customer going to be inserted
				removed.remove(removed.size()-1);// remove customer from the list 
				Point ins = null; // best customer where it is going to be inserted
				int route = 0;
				boolean inserted = false;
				boolean before = false; // before or after customer
				//boolean regular = true;
				double best_improvement = Double.MAX_VALUE; // best insertion value so far
				
				for(int k = 0; k < sol.routes_0.size(); k ++) {
					if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[0]) {
						continue;
					}
					int r_type = sol.routes_0.get(k).type;
					boolean CD_to_PV = false;
					if(r_type > 0){
						if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[1]) {
							CD_to_PV =true;
						}
					}
					
					//double route_duration = all_customers.get(sol.routes1.get(k).last()).get_r_d();
					
					double time = 0.0;
					//double distance = sol.routes1.get(k).distance;
					Point p = sol.routes_0.get(k).start;
					double cost = insert_cost(p.prev, cus1, p);
					cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					//if(distance + cost > route_duration) {}
					if(noise(cost,best_improvement)) {
						//check if feasible 
						if(feasible_insert(cus1, time, p.prev, p)) {
							best_improvement = cost;
							ins = p;
							before = true;
							route = k;
							inserted = true;
						}	
					}
					
					while(p.next != null) {
						//if(p.prev!= null) {
						//	distance += Cost[p.prev.id][p.id];
						//	}else {distance += Cost[depot_start.id][p.id];}
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						cost = insert_cost(p, cus1, p.next);
						cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
						if(noise(cost, best_improvement)) {
							//check if feasible 
							if(feasible_insert(cus1, time, p, p.next)) {
								best_improvement = cost;
								ins = p.next;
								before = true;
								route = k;
								inserted = true;
							}
						}
						p = p.next;
					}
					cost = insert_cost(p, cus1, p.next);
					cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					if(noise(cost, best_improvement)) {
						//check if feasible 
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						if(feasible_insert(cus1, time, p, p.next)) {
							best_improvement = cost;
							ins = p;
							before = false;
							route = k;
							inserted = true;
						}	
					}
				}
				//insert at ins
				if(inserted) {
					
					if(before) {
						
						if(ins != null) {
							if(ins.prev!=null) {
								cus1.next = ins;
								cus1.prev = ins.prev;
								ins.prev.next=cus1;
								ins.prev=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								
							}else {
								
								ins.prev=cus1;
								cus1.next = ins;
								cus1.prev = null;
						
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								sol.routes_0.get(route).start = cus1;
							}
						}
					}else {
						
						if(ins != null) {
							if(ins.next!=null) {
								cus1.prev = ins;
								cus1.next = ins.next;
								ins.next.prev=cus1;
								ins.next=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}else {
								ins.next=cus1;
								cus1.next = null;
								cus1.prev = ins;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}
						}	
					}
				}else {
					sol.routes_0.add(new Route(cus1));
				}
			}
			
		}
		private void insert_CD(int q, Solution sol) {
			while(removed.size() > 0) {
				//insert
				Point cus1 = sol.cus[removed.get(removed.size()-1)];//customer going to be inserted
				removed.remove(removed.size()-1);// remove customer from the list 
				Point ins = null; // best customer where it is going to be inserted
				int route = 0;
				boolean inserted = false;
				boolean before = false; // before or after customer
				//boolean regular = true;
				double best_improvement = Double.MAX_VALUE; // best insertion value so far
				for(int o = 0; o < 2; o++) {//cd first then pv
					if(inserted) {break;}
				for(int k = 0; k < sol.routes_0.size(); k ++) {
					if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[0]) {
						continue;
					}
					int r_type = sol.routes_0.get(k).type;
					if(r_type==0) {
						if(o<1) {
							continue;
						}
					}else if(o>0){
							continue;
					}
					boolean CD_to_PV = false;
					if(r_type > 0){
						if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[1]) {
							CD_to_PV =true;
						}
					}
					
					//double route_duration = all_customers.get(sol.routes1.get(k).last()).get_r_d();
					
					double time = 0.0;
					//double distance = sol.routes1.get(k).distance;
					Point p = sol.routes_0.get(k).start;
					double cost = insert_cost(p.prev, cus1, p);
					cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					//if(distance + cost > route_duration) {}
					if(cost < best_improvement) {
						//check if feasible 
						if(feasible_insert(cus1, time, p.prev, p)) {
							best_improvement = cost;
							ins = p;
							before = true;
							route = k;
							inserted = true;
						}	
					}
					
					while(p.next != null) {
						//if(p.prev!= null) {
						//	distance += Cost[p.prev.id][p.id];
						//	}else {distance += Cost[depot_start.id][p.id];}
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						cost = insert_cost(p, cus1, p.next);
						cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
						if(cost < best_improvement) {
							//check if feasible 
							if(feasible_insert(cus1, time, p, p.next)) {
								best_improvement = cost;
								ins = p.next;
								before = true;
								route = k;
								inserted = true;
							}
						}
						p = p.next;
					}
					cost = insert_cost(p, cus1, p.next);
					cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					if(cost < best_improvement) {
						//check if feasible 
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						if(feasible_insert(cus1, time, p, p.next)) {
							best_improvement = cost;
							ins = p;
							before = false;
							route = k;
							inserted = true;
						}	
					}
				}
				}
				//insert at ins
				if(inserted) {
					
					if(before) {
						
						if(ins != null) {
							if(ins.prev!=null) {
								cus1.next = ins;
								cus1.prev = ins.prev;
								ins.prev.next=cus1;
								ins.prev=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								
							}else {
								
								ins.prev=cus1;
								cus1.next = ins;
								cus1.prev = null;
						
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								sol.routes_0.get(route).start = cus1;
							}
						}
					}else {
						
						if(ins != null) {
							if(ins.next!=null) {
								cus1.prev = ins;
								cus1.next = ins.next;
								ins.next.prev=cus1;
								ins.next=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}else {
								ins.next=cus1;
								cus1.next = null;
								cus1.prev = ins;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}
						}	
					}
				}else {
					sol.routes_0.add(new Route(cus1));
				}
			}
			
		}
		private void insert_PV(int q, Solution sol) {
			while(removed.size() > 0) {
				//insert
				Point cus1 = sol.cus[removed.get(removed.size()-1)];//customer going to be inserted
				removed.remove(removed.size()-1);// remove customer from the list 
				Point ins = null; // best customer where it is going to be inserted
				int route = 0;
				boolean inserted = false;
				boolean before = false; // before or after customer
				//boolean regular = true;
				double best_improvement = Double.MAX_VALUE; // best insertion value so far
				for(int o = 0; o < 2; o++) {//cd first then pv
					if(inserted) {break;}
				for(int k = 0; k < sol.routes_0.size(); k ++) {
					if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[0]) {
						continue;
					}
					int r_type = sol.routes_0.get(k).type;
					if(r_type>0) {
						if(o<1) {
							continue;
						}
					}else if(o>0){
							continue;
					}
					boolean CD_to_PV = false;
					if(r_type > 0){
						if(sol.routes_0.get(k).load + all_customers.get(cus1.id).d() > Parameters.capacity[1]) {
							CD_to_PV =true;
						}
					}
					//if(CD_to_PV) {continue;}
					
					//double route_duration = all_customers.get(sol.routes1.get(k).last()).get_r_d();
					
					double time = 0.0;
					//double distance = sol.routes1.get(k).distance;
					Point p = sol.routes_0.get(k).start;
					double cost = insert_cost(p.prev, cus1, p);
					cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
					//if(distance + cost > route_duration) {}
					if(cost < best_improvement) {
						//check if feasible 
						if(feasible_insert(cus1, time, p.prev, p)) {
							best_improvement = cost;
							ins = p;
							before = true;
							route = k;
							inserted = true;
						}	
					}
					
					while(p.next != null) {
						//if(p.prev!= null) {
						//	distance += Cost[p.prev.id][p.id];
						//	}else {distance += Cost[depot_start.id][p.id];}
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						cost = insert_cost(p, cus1, p.next);
						cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference,sol.routes_0.get(k).distance);
						if(cost < best_improvement) {
							//check if feasible 
							if(feasible_insert(cus1, time, p, p.next)) {
								best_improvement = cost;
								ins = p.next;
								before = true;
								route = k;
								inserted = true;
							}
						}
						p = p.next;
					}
					cost = insert_cost(p, cus1, p.next);
					cost = s_cost(cost, CD_to_PV, sol.routes_0.get(k).preference, sol.routes_0.get(k).distance);
					if(cost < best_improvement) {
						//check if feasible 
						time = time_to_p(time, p.prev, p);
						time += all_customers.get(p.id).time_at_node();
						if(feasible_insert(cus1, time, p, p.next)) {
							best_improvement = cost;
							ins = p;
							before = false;
							route = k;
							inserted = true;
						}	
					}
				}
				}
				//insert at ins
				if(inserted) {
					
					if(before) {
						
						if(ins != null) {
							if(ins.prev!=null) {
								cus1.next = ins;
								cus1.prev = ins.prev;
								ins.prev.next=cus1;
								ins.prev=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								
							}else {
								
								ins.prev=cus1;
								cus1.next = ins;
								cus1.prev = null;
						
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
								sol.routes_0.get(route).start = cus1;
							}
						}
					}else {
						
						if(ins != null) {
							if(ins.next!=null) {
								cus1.prev = ins;
								cus1.next = ins.next;
								ins.next.prev=cus1;
								ins.next=cus1;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}else {
								ins.next=cus1;
								cus1.next = null;
								cus1.prev = ins;
								sol.routes_0.get(route).load += all_customers.get(cus1.id).d();
							}
						}	
					}
				}else {
					sol.routes_0.add(new Route(cus1));
				}
			}
			
		}
		private boolean accept(Solution sol) {
			
			//if(sol.get_solution() - solution.get_solution()<0) {return true;}else {return false;}
			double e = 1/Math.exp((sol.get_solution() - solution.get_solution())/Temperature);
			//double e = 1/Math.exp((sol.get_solution() - solution.get_solution())/(solution.s_value*0.005));
			double r = Math.random();
			//if(curr_it%3==0 && e < 1.0) {e=0.0;}
			//System.out.println(e+ " f " + sol.get_solution() + " h " + solution.get_solution());
			//if((sol.get_solution() - solution.get_solution())/(solution.s_value*0.02) < r ) {
			if(e > r ) {
				return true;
			}else {return false;}
			//if(r<e){
			//	return true;
			//}else {
			//	return false;
			//}
		}

		private double s_cost(double c, boolean cP, int pre, double d) {
			if(cP) {
				return c + f_cost(0) - f_cost(pre) + d - d*v_cost(pre);
			}else {
				return c*v_cost(pre);
			}
		}
		private double dcm_cost(double c, boolean cP, double demand, double geo) {
			if(cP) {
				return c;
			}else {
				return c*Parameters.DCMBetas[0] + demand*Parameters.DCMBetas[1] + Parameters.DCMBetas[2] + geo*Parameters.DCMBetas[3]; //distance beta, load beta, size beta, geographical beta
			}
		}
		private boolean noise(double c, double b) {
			int n = rand.nextInt(2);
			int n1 = rand.nextInt(2);
			double p = 0.105;
			if(c < b + c*p*(n-n1)) {
				return true;
			}else {
				return false;
			}
		}
		private boolean feasible_insert(Point p_in ,double t, Point p1, Point p2 ) {
			//return true;
			//use only for time windows
			boolean feasible = false;
			if(p1!=null ) {
				p1.next = p_in;
				p_in.next = p2;
				feasible = feasible_recursive(t, p1, p_in);
				p_in.next = null;
				p1.next = p2;	
			} else if(p1 == null) {
				p_in.next = p2;
				feasible = feasible_recursive(t, p1, p_in);
				p_in.next = null;
			}
			return feasible;
		}
		private boolean feasible_recursive(double time, Point p1, Point p2 ) {
			time = time_to_p(time, p1 ,p2);
			if(p2==null) {
				return true;
			}
			else if( time > all_customers.get(p2.id).b()) {
				return false;
			}else if(p2.next !=null){
				time += all_customers.get(p2.id).time_at_node();
				return feasible_recursive(time, p2, p2.next);
			}else {
				return true;
			}
			
		}
		
		private double insert_cost(Point p1, Point p3, Point p2) {
			if(p1==null && p2!=null) {
				return Cost[depot_start.id][p3.id] + Cost[p3.id][p2.id] - Cost[depot_start.id][p2.id];
			}else if(p1!=null && p2!=null ){
				return Cost[p1.id][p3.id] + Cost[p3.id][p2.id] - Cost[p1.id][p2.id];
			}else if(p1!=null && p2==null) {
				return Cost[p1.id][p3.id] + Cost[p3.id][depot_start.id] - Cost[p1.id][depot_start.id];
			}else {
				return Cost[depot_start.id][p3.id] + Cost[p3.id][depot_start.id];
			}	
		}
	}
	private class Point{
		public int id;
		public Point prev;
		public Point next;
		public Point(int i){
			id = i;
			prev = null;
			next = null;
		}
		public Point (Point po) {
			this.id = po.id;
			this.prev = po.prev;
			this.next = po.next;
		}
	}
	public class Solution{
		public double s_value = Double.MAX_VALUE;
		public List<Route> routes_0 = new ArrayList<>();
		Point [] cus;
		public Solution(Solution sol) {
			this.cus = new Point [num_customers];
			this.s_value = sol.s_value;
			for(int k = 0 ; k < sol.routes_0.size(); k++) {
				Route r = new Route(sol.routes_0.get(k));
				this.routes_0.add(r);
				Point p = r.start;
				this.cus[p.id] = r.start;
				while(p.next!=null) {
					Point p2 = new Point(p.next);
					p.next = p2;
					p2.prev = p;
					p=p2;
					this.cus[p.id] = p;
				}
			}	
		}
		public Solution(double v ,List<Route> routes_1){
			this.cus = new Point [num_customers];
			this.s_value = v;
			this.routes_0 = routes_1;
			for(int k = 0 ; k < routes_1.size(); k++) {
				Point p = routes_0.get(k).start;
				this.cus[p.id] = p;
				while(p.next!=null) {
					p=p.next;
					this.cus[p.id] = p;
				}
			}
			update_vehicle(1);
			get_solution();
		}public int route_type(double l) {
			if(l<Parameters.capacity[1]) {
				return 1;
			}else {
				return 0;
			}
		}
		
		public void update_vehicle(int b) {
			s_value = 0.0;
			int cD = 0;
			Collections.sort(this.routes_0);
			for(int k = 0; k < routes_0.size(); k ++) {
				routes_0.get(k).size();
				routes_0.get(k).type = route_type(routes_0.get(k).load);
				if(routes_0.get(k).type == 1 && (cD < Parameters.CD_max)) {
					cD +=1;
					routes_0.get(k).cost = routes_0.get(k).distance*v_cost(cD);
					routes_0.get(k).cost += f_cost(cD);
					routes_0.get(k).preference = cD;
				}else {
					routes_0.get(k).cost = routes_0.get(k).distance*v_cost(0);
					routes_0.get(k).cost += f_cost(0);
					routes_0.get(k).type = 0;
				}
				s_value+=routes_0.get(k).cost;
			} 
			//System.out.println("Solution update vehicle = " + s_value +  " num vehicles " + routes_0.size() + " CD---" + cD);
		}

		public void SAA_MonteCarlo(int b) { //Sample Average Approximation. 
			s_value = 0.0;
			int cD = 0;
			Collections.sort(this.routes_0);
			for(int k = 0; k < routes_0.size(); k ++) {
				routes_0.get(k).size();
				routes_0.get(k).type = route_type(routes_0.get(k).load);
				if(routes_0.get(k).type == 1 && (cD < Parameters.CD_max || b < 1)) {
					cD +=1;
					routes_0.get(k).cost = routes_0.get(k).distance*v_cost(cD);
					routes_0.get(k).cost += f_cost(cD);
					routes_0.get(k).preference =cD;
				}else {
					routes_0.get(k).cost = routes_0.get(k).distance*v_cost(0);
					routes_0.get(k).cost += f_cost(0);
					routes_0.get(k).type = 0;
				}
				s_value+=routes_0.get(k).cost;
			} 
			//System.out.println("Solution update vehicle = " + s_value +  " num vehicles " + routes_0.size() + " CD---" + cD);
		}

		public void priceroute(int b) { 
			s_value = 0.0;
			int cD = 0;
			for(int k = 0; k < routes_0.size(); k ++) {
				routes_0.get(k).size();// organizes the route k
				routes_0.get(k).type = route_type(routes_0.get(k).load);
				if(routes_0.get(k).type == 1 && (cD < Parameters.CD_max || b < 1)) {
					cD +=1;
					double mya = 1/Parameters.DCMBetas[4];
					double myb = routes_0.get(k).U_notprice/Parameters.DCMBetas[4];
					double myc = routes_0.get(k).distance;
					Goldensection H = new Goldensection(mya, myb, myc);
					double V = H.goldenSectionSearch(0, 50);
					routes_0.get(k).V_tility = V;
					routes_0.get(k).cost = (Math.exp(V)/(Math.exp(V) +1))*(V+routes_0.get(k).U_notprice)/Parameters.DCMBetas[4];
				}else {
					routes_0.get(k).cost = routes_0.get(k).distance*v_cost(0);
					routes_0.get(k).cost += f_cost(0);
					routes_0.get(k).type = 0;
				}
				s_value+=routes_0.get(k).cost;
			} 
			//System.out.println("Solution update vehicle = " + s_value +  " num vehicles " + routes_0.size() + " CD---" + cD);
		}

		public double get_solution() {
			double cost = 0.0;
			for (int k = 0; k < routes_0.size(); k ++) {
				cost += routes_0.get(k).cost;
			}
			s_value = cost;
			//System.out.println("Solution get = " + s_value);
			return s_value;
		}
		public int get_vehicles() {
			return routes_0.size();
		}
		public int get_cd() {
			int vehi = 0;
			for(int k = 0; k<routes_0.size(); k++) {
				if(routes_0.get(k).type>0) {	
					vehi+=1;
				}
			}
			return vehi;
		}
	}
	private class Route implements Comparable<Route>{
		public Point start = null;
		public int preference=0;
		public int type;
		public double load;//total load of the route
		public double cost;// the total cost based on the distance. 
		public double U_notprice; // the utility of the route based on the discrete choice model.
		public double V_tility; 
		public double geo;// The total value of the geographical location of customers. 
		public double distance; // Total distance of routes. 
		public int size; // Total number of customers. 
		public Point end = null;
		public Route(Point p) {
			type = 0;
			start = p;
			load = all_customers.get(p.id).d();
			size();
		}
		public Route(Route r) {
			this.load = r.load;
			this.cost = r.cost;
			this.size = r.size;
			this.geo = r.geo;
			this.type = r.type;
			this.U_notprice = r.U_notprice;
			this.preference = r.preference;
			this.distance = r.distance;
			start = new Point(r.start);
			size();
		}
		public int first() {
			return start.id;
		}
		public int last() {
			Point p = start;
			this.distance = Cost[depot_start.id][p.id];
			while(p.next!= null) {
				this.distance += Cost[p.id][p.next.id];
				p=p.next;
			}
			this.end = p;
			return p.id;
		}
		public int size() {
			Point p = start;
			double demand = all_customers.get(p.id).d();
			int s = 1;
			double g = all_customers.get(p.id).geo;
			this.distance = Cost[depot_start.id][p.id];
			while(p.next!= null) {
				this.distance += Cost[p.id][p.next.id];
				s+=1;
				demand += all_customers.get(p.next.id).d();
				p = p.next;
				g += all_customers.get(p.id).geo;
			}
			this.distance += Cost[p.id][depot_start.id]; //return to depot.
			end = p;
			load = demand;
			size = s;
			geo = g;
			U_notprice = distance*Parameters.DCMBetas[0] + load*Parameters.DCMBetas[1] + size*Parameters.DCMBetas[2] + geo*Parameters.DCMBetas[3];
			return s;
		}

		public double getDistance() {
			return this.distance;
		}

		public double getUwithoutprice(){
			return this.U_notprice;
		}

		public boolean feasible() {
			boolean feasible = true;
			double distance = Cost[depot_start.id][first()];
			double time = Math.max(distance, all_customers.get(first()).a()); 		
			this.load = (int)all_customers.get(first()).d();
			double Q_1 = Parameters.capacity[this.type];
			if(load > Q_1) {
				return false;
			}
			else if(time > all_customers.get(first()).b()) {
				return false;
			}
			time += all_customers.get(first()).time_at_node();
			Point p = start;
			while(p.next != null) {
				distance += Cost[p.id][p.next.id];
				time+= Cost[p.id][p.next.id];
				load += (int)all_customers.get(p.next.id).d();
				if(load > Q_1) {
					System.out.println("Type -- load " + this.type + " -- "+ load);
					return false;
				}else if(time > all_customers.get(p.next.id).b()) {
					System.out.println("time " + time + " max = " + all_customers.get(p.next.id).b());
					return false;
				}else {
					time = Math.max(time, all_customers.get(p.next.id).a()) 
						+ all_customers.get(p.next.id).time_at_node();
				}
				p = p.next;
			}
			distance += Cost[depot_start.id][last()];
			time += Cost[depot_start.id][last()];		
			if(time > 1236  ) {
				return false;
			}
			System.out.println(" distance " + distance + " --- ");
		return feasible;
		}
		@Override
	    public int compareTo(Route member) {
	        if (this.distance == member.getDistance()) {
	            return 0;
	        } else if (this.distance > member.getDistance()) {
	            return -1;
	        } else {
	            return 1;
	        }
	    }
	    @Override
	    public String toString() {
	        return "[ Type =" + type + ", distance =" + distance + ", preference" + preference + "]";
	    }
	}
	private class Node {
		public int id;
		public int id_external;
		private double xcoord;
		private double ycoord;
		private double t_at_node;
		
		public Node(int external_id, double x, double y, double t) {
			this.id = all_nodes.size();
			this.id_external = external_id;
			this.xcoord = x;
			this.ycoord = y;
			this.t_at_node = t;
			all_nodes.put(this.id, this);
		}
		public double time_to_node(Node node_to) {
			return Math.sqrt(Math.pow(this.xcoord-node_to.xcoord, 2)+Math.pow(this.ycoord-node_to.ycoord, 2));
		}
		public double time_at_node() {
			return t_at_node;
		}
		public double getxcoord() {
			return this.xcoord;
		}
		public double getycoord() {
			return this.ycoord;
		}
	}
	private class Depot extends Node {
		public Depot(int external_id, double x, double y) {
			super(external_id,x,y,0);
		}
	}
	private class Customer extends Node{
		private double demand;
		private double ready_time;
		private double due_date;
		private int cluster;
		private double geo;
		private double r_d;
		public Customer(int external_id, double x, double y, double demand, double ready_time, double due_date, double service_time) {
			super(external_id,x,y,service_time);
			this.demand = demand;
			this.ready_time = ready_time;
			this.due_date = due_date;
			all_customers.put(this.id, this);
		}
		public void set_r_d(double c) {
			this.r_d = c;;
		}
		public double get_r_d() {
			return this.r_d;
		}
		public void set_cluster(int c) {
			this.cluster = c;
		}
		public void set_geo(double g) {
			this.geo = g;
		}
		public int get_cluster() {
			return cluster;
		}
		public double a() {
			return ready_time;
		}
		public double b() {
			return due_date;
		}
		public double d() {
			return demand;
		}
		
	}
	public class C_W {
		public C_W(){
			
		}
		public Solution begin() {
			boolean merged =  true;
			List<Route> routes1 = new ArrayList<>();
			for(int i = 0; i < num_customers; i ++) {
				Point p = new Point(i);
				routes1.add(new Route(p));
			}
			while (merged) {
				merged = false;
				int y = 0;
				int t = 0;
				double best = 0.0;
				for(int k = 0; k < routes1.size(); k ++) {
					Point p = routes1.get(k).start;
					int demand = 0;
					double ti = Cost[depot_start.id][p.id];
			
					while(p.next != null) {
						ti = Math.max(all_customers.get(p.id).a(), ti);
						demand += (int)all_customers.get(p.id).d();
						ti += all_customers.get(p.id).time_at_node();
						ti += Cost[p.id][p.next.id];
						p = p.next;
					}
					ti = Math.max(all_customers.get(p.id).a(), ti);
					demand += (int)all_customers.get(p.id).d();
					ti += all_customers.get(p.id).time_at_node();
					
					for(int k2 = 0 ; k2 < routes1.size(); k2++) {
						if(k2==k) {continue;}
						Point p2 = routes1.get(k2).start;
						double cs = cost_savings(p.id, p2.id);
						
						if(cost_savings(p.id, p2.id) > best + 0.00001){
							// check if feasible
							boolean feasible = true;
							int demand2 = demand;
							double ti2 = ti + Cost[p.id][p2.id];
							while(p2.next != null && feasible){
								ti2 = Math.max(all_customers.get(p2.id).a(), ti2);
								demand2 += (int)all_customers.get(p2.id).d();
								if(ti2 > all_customers.get(p2.id).b()) {
									feasible = false;
								}
								if(demand2 > Q) {
									feasible = false;
								}
								ti2 += all_customers.get(p2.id).time_at_node();
								ti2 += Cost[p2.id][p2.next.id];
								p2 = p2.next;
							}
							ti2 = Math.max(all_customers.get(p2.id).a(), ti2);
							demand2 += (int)all_customers.get(p2.id).d();
							if(ti2 > all_customers.get(p2.id).b()) {
								feasible = false;
							}
							if(demand2 > Q) {
								feasible = false;
							}
							if(feasible) {
								y = k;
								t = k2;
								best = cs;
								merged = true;
							}
						}
					}
				}
				if(merged) {
					merge(routes1.get(y).start,routes1.get(t).start);
					Route r = new Route(routes1.get(y).start);
					r.load = routes1.get(y).load + routes1.get(t).load;
					routes1.add(r);
					if(y>t) {
						routes1.remove(y);
						routes1.remove(t);
					}else {
						routes1.remove(t);
						routes1.remove(y);
					}
				}
			}
			return new Solution(0.0,routes1);	
		}
		private void merge(Point start, Point end) {
			Point p = start;
			while(p.next !=null) {
				p = p.next;
			}
			p.next = end;
			end.prev = p;
		}
		private double cost_savings(int i , int j) {
			return (Cost[depot_start.id][i] + Cost[depot_start.id][j]- Cost[i][j]);
		}
		public boolean feasible_list(List<Integer> c, int type) {
			boolean feasible =  true;
			int rsizef = c.size();
			int demand = 0;
			double ti = Cost[depot_start.id][c.get(0)];
			for(int i = 0 ; i < rsizef; i++ ) {
				ti = Math.max(all_customers.get(c.get(i)).a(), ti);
				demand += (int)all_customers.get(c.get(i)).d();
				if(ti > all_customers.get(c.get(i)).b()) {
					return false;
				}
				if(demand > Q) {
					return false;
				}
				if(i < rsizef-1) {
					ti += all_customers.get(c.get(i)).time_at_node();
					ti += Cost[c.get(i)][c.get(i+1)];
				}
			}
			return feasible;
		}
	}
	private double time_to_p(double t, Point p1 , Point p2) {
		if(p1==null && p2 !=null) {
			return Math.max(t + Cost[depot_start.id][p2.id], all_customers.get(p2.id).a());
		}else if(p1!=null && p2!=null){
			return Math.max(t + Cost[p1.id][p2.id], all_customers.get(p2.id).a());
		}else if(p1!=null && p2 == null) {
			return t + Cost[p1.id][depot_start.id];
		}else {
			return Double.MAX_VALUE;
		}	
	}
	public int get_cluster(int c) {
		for(int clu = 0; clu < all_clusters.size(); clu++) {
			List<Integer> kkk = all_clusters.get(clu).cluster_customers;
			for(int cu = 0; cu < kkk.size();cu++) {
				if(c == kkk.get(cu)) {
					return clu;
				}
			}
		}
		System.out.println(" get_cluster() failed, no cluster found for customer \'c\' ");
		return 0;
	}
	
	public void create_clusters() {
		List<int[]> clusters = new ArrayList<int[]>();
		int [] cluster_1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ,11, 75};
		clusters.add(cluster_1);
		int [] cluster_2= {12, 13, 14, 15, 16, 17, 18, 19};
		clusters.add(cluster_2);
		int [] cluster_3 = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
		clusters.add(cluster_3);
		int [] cluster_4 = {31, 32, 33, 34, 35, 36, 37, 38, 39};
		clusters.add(cluster_4);
		int [] cluster_5 = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52};
		clusters.add(cluster_5);
		int [] cluster_6 = {53, 54, 55, 56, 57, 58, 59, 60};
		clusters.add(cluster_6);
		int [] cluster_7 = {61, 62, 63, 64, 65, 66, 67, 68, 69, 72, 74};
		clusters.add(cluster_7);
		int [] cluster_8 = {70, 71, 73, 76, 77, 78, 79, 80, 81};
		clusters.add(cluster_8);
		int [] cluster_9 = {82, 83, 84, 85, 86, 87, 88, 89, 90, 91};
		clusters.add(cluster_9);
		int [] cluster_10 = {92, 93, 94, 95, 96, 97, 98, 99, 100};
		clusters.add(cluster_10);
		for(int i = 0; i < clusters.size(); i ++) {
			List<Integer> temp = new ArrayList<>();
			boolean atleastone = false;
			for(int j = 0; j < clusters.get(i).length; j++) {
				if(clusters.get(i)[j] <= Parameters.num_customers) {
					atleastone = true;
					int customer_internal = clusters.get(i)[j]-1;
					temp.add(customer_internal);
				}
			}
			if(atleastone) {
				new Cluster(temp);
			}
		}
	}
	public void print_routes() {
		System.out.println("incumbent " + incumbent.get_solution() + "  vehicles " + incumbent.get_vehicles());
		for(int r = 0; r < incumbent.routes_0.size(); r++) {
			if(incumbent.routes_0.get(r).feasible()) {
				System.out.print(" Feasible type load = " + incumbent.routes_0.get(r).type + " -- "+ incumbent.routes_0.get(r).load + " ");
			}else { System.out.print(" Not Feasible  ");}
			Point p = incumbent.routes_0.get(r).start;
			System.out.print(" Depot --> ");
			while(p != null) {
				System.out.print(", " + p.id);
				p = p.next;
			}
			System.out.println(",");
		}
	}
	public void initiate(int sto) {
		ALNS_1 problem = new ALNS_1();
		problem.iterations = 700000;
		for(int i = 0 ; i < problem.iterations; i++) {
			problem.curr_it = i;
			problem.LNS();
		}

		//System.out.println("Wheel size =  " + problem.wheel.size() + " tem = " + problem.Temperature);
		//problem.update_wheel(10);
		//System.out.println("Wheel size =  " + problem.wheel.size() + " tem = "  + problem.Temperature + " sol " + incumbent.get_solution());
	}

	private void fill_cost() {
		Cost = new double [num_customers +1][num_customers +1];
		//fill cost matrix with the distance between customers.
		for (int i : all_nodes.keySet()) {
			for (int j : all_nodes.keySet()) {
				Cost[i][j] = all_nodes.get(i).time_to_node(all_nodes.get(j));
			}
		}
	}
	private void fill_nearest() {
		//matrix with nearest neighbors
		Nearest = new int [num_customers+1][num_customers+1];
		for (int i : all_nodes.keySet()) {
			Nearest[i][0]=i;
			for (int j = 1; j <=num_customers; j++) {
				if(i == j) {
					Nearest[i][j]=0;
				}else {
					Nearest[i][j] =j;
				}
			}
		}
		for (int i : all_nodes.keySet()) {
			int v1 =0;
			int v2=0;
			for (int j = 1; j <= num_customers; j++) {
				double min = Double.MAX_VALUE;
				v1 = Nearest[i][j];
				v2 = j;
				for (int n  = j; n <= num_customers; n++) {
					if(Cost[i][Nearest[i][n]] < min) {
						v2 = n;
						min = Cost[i][Nearest[i][n]];
					}
				}
				Nearest[i][j] = Nearest[i][v2]; 
				Nearest[i][v2] = v1;
			}
		}
	}private void fill_demand_array() {
		//matrix with nearest neighbors
		Demand_Array = new double [num_customers+1];
		Time_e_Array = new double [num_customers+1];
		Time_l_Array = new double [num_customers+1];
		Time_Service = new double [num_customers+1];

		for (int i : all_customers.keySet()) {
			Time_e_Array[i] = all_customers.get(i).a();
			Time_l_Array[i] = all_customers.get(i).b();
			Time_Service[i] = all_customers.get(i).time_at_node();
			Demand_Array[i] = all_customers.get(i).d();
		}

		int i = depot_start.id;
		Time_e_Array[i] = 0.0;
		Time_l_Array[i] = 2000;
		Time_Service[i] = depot_start.time_at_node();
		Demand_Array[i] = 0.0;
	}

	private void ReadData() {
		try {
			// We need to provide file path as the parameter: 
		  	//Read the instance
			String ins_path = "/home/ftorres/Desktop/VRP/LNS/In2/" + instance+".txt";
			//String ins_path = "/Users/fabiantorres/Desktop/instances/" + instance+".txt";
		  	File file = new File(ins_path);
			int id_external = 0;
			double xcoord = 0;
			double ycoord = 0;
			double demand = 0;
			double readyt = 0;
			double duedate = 0;
			double servicet = 0;
			BufferedReader br = null;
			FileReader r = new FileReader(file);
			br = new BufferedReader(r); 
		 	double xc = 0;
		 	double yc = 0;
		  	String st;
		  	int count = 0;
		  	while ((st = br.readLine()) != null){
		    	count += 1;
		    	if(count > 9 && this.num_customers  + 11 > count){
					//System.out.println(st);
					var x = "";
					int word = 0;
					for (int i = 0; i < st.length();i++){
						char c = st.charAt(i);
			    		if(Character.isDigit(c)){
			    			x+= c; 
						}if(!Character.isDigit(c)|| i == st.length() -1){
							if(x.length()>0 ){
								if(word == 0){id_external = Integer.parseInt(x);}
								if(word == 1){xcoord = Double.parseDouble(x);}
								if(word == 2){ycoord = Double.parseDouble(x);}
								if(word == 3){demand = Double.parseDouble(x);}
								if(word == 4){readyt = Double.parseDouble(x);}
								if(word == 5){duedate = Double.parseDouble(x);}
								if(word == 6){servicet = Double.parseDouble(x);}
								//System.out.print("value x = "+x+"--" +Double.parseDouble(x));}
								word+=1;x="";
							}
						}	
					}
					if(count == 10) {
						xc = xcoord;
						yc = ycoord;
							
					}else {
						new Customer(id_external, xcoord, ycoord, demand, readyt, duedate, servicet);
					}
				}
		  	}
		    depot_start = new Depot(0,xc,yc);
		  	br.close();
		}
		catch (Exception ex) {
            ex.printStackTrace();
        }
	}
}