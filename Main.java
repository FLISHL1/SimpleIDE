public class Main{
	public static void main(String... args){
		String a = "asd";
		Integer b = 1;
		int c = 0;
		
		
		System.out.print(a + b + a + Test.hello());
	}
	public static class Test{
		public static String hello(){
			return "Hello World";
		}
	}
}
