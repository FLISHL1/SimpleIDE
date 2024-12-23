public class Main{
	public static void main(String... args){
		String a = "asd";
		
		Integer b = 1;
		Test t = new Test();
		t.hello();
		int c = 0;
	 	
		System.out.print(a + b + a + t.hello());
	}
	public static class Test{
		public static String hello(){
			return "Hello World";
		}
	}
}
