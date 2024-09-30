
public class Goldensection {
    public double a;
    public static double tolerance = 1e-5;
    private static double ma,mb,mc;

    public Goldensection(double mya, double myb, double myc){
        ma = mya;
        mb = myb;
        mc = myc;
    }

    // The golden ratio
    
    private static final double R = (Math.sqrt(5) - 1) / 2;

    // Example function to minimize (replace with your function)

    private static double f(double x) {
        return ma*x*(Math.exp(x)/(Math.exp(x)+1)) + mb*(Math.exp(x)/(Math.exp(x)+1))  - mc*(Math.exp(x)/(Math.exp(x)+1))  + mc;
    }

    public double getf(double x){
        return ma*x*(Math.exp(x)/(Math.exp(x)+1)) + mb*(Math.exp(x)/(Math.exp(x)+1))  - mc*(Math.exp(x)/(Math.exp(x)+1))  + mc;
    }

    // Golden Section Search Method to find minimum of f in [a, b]
    public double goldenSectionSearch(double a, double b) {
        double x1, x2;

        // Initialize x1 and x2 based on the golden ratio
        x1 = b - R * (b - a);
        x2 = a + R * (b - a);

        while (Math.abs(b - a) > tolerance) {
            if (f(x1) > f(x2)) {
                a = x1;
                x1 = x2;
                x2 = a + R * (b - a);
            } else {
                b = x2;
                x2 = x1;
                x1 = b - R * (b - a);
            }
        }

        // Approximate the minimum point
        return (a + b) / 2;
    }
}
