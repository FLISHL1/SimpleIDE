import java.io.File;

public class Main {
	
	public static void main(String... args){
		String a = "asd";
		Integer b = 1;
		int c = 0;
		Test t = new Test();
		System.out.print(a + b + a + t.hello());
	}
	public static class Test{
		public String h = "Hellol=";
		public String hello(){
			return h + "World";
		}
	}
	
}

