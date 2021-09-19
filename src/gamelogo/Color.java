package gamelogo;
import java.util.Arrays;

import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.core.LogoList;


public abstract class Color {
	
	public abstract Object getValue();
	
	public static Color parseColor(String str) throws ExtensionException {
		str = str.trim();
		try {
			return new DoubleColor(Double.parseDouble(str));
		} catch (Exception e) { }
		try {
			String[] vals = str.substring(1, str.length()-1).split(" ");
			Double r = Double.parseDouble(vals[0].trim());
			Double g = Double.parseDouble(vals[1].trim());
			Double b = Double.parseDouble(vals[2].trim());
			return new ListColor(r, g, b);
		} catch (Exception e) { }
		throw new ExtensionException("\""+str+"\" is not a valid color");
	}
	
	public static Color asColor(Object obj) throws ExtensionException {
		if (obj instanceof Double) return new DoubleColor((Double)obj);
		if (obj instanceof LogoList) return new ListColor((LogoList)obj);
		throw new ExtensionException(obj+" is not a valid color");
	}
	
	public static class DoubleColor extends Color {
		
		double value;
		
		@Override
		public Double getValue() {
			return value;
		}
		
		public DoubleColor(double value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			return Double.toString(value);
		}
		
	}
	
	public static class ListColor extends Color {
		
		LogoList value;
		
		@Override
		public LogoList getValue() {
			return value;
		}
		
		public ListColor(double r, double g, double b) {
			this.value = LogoList.fromJava(Arrays.asList(r, g, b));
		}
		public ListColor(LogoList list) {
			this.value = list;
		}
		
		@Override
		public String toString() {
			return Dump.logoObject(value);
		}
		
	}
}