package listener.main;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;


import generated.*;

public class Translator {
	enum OPTIONS {
		PRETTYPRINT, BYTECODEGEN, UCODEGEN, ERROR
	}
	private static OPTIONS getOption(String[] args){
		if (args.length < 1)
			return OPTIONS.BYTECODEGEN;

		if (args[0].startsWith("-p")
				|| args[0].startsWith("-P"))
			return OPTIONS.PRETTYPRINT;

		if (args[0].startsWith("-b")
				|| args[0].startsWith("-B"))
			return OPTIONS.BYTECODEGEN;

		if (args[0].startsWith("-u")
				|| args[0].startsWith("-U"))
			return OPTIONS.UCODEGEN;

		return OPTIONS.ERROR;
	}

	public static void main(String[] args) throws Exception
	{
		CharStream codeCharStream = CharStreams.fromFileName("C:/Users/82103/Desktop/2019_Compiler_TeamProject/HW05_3/test.c");
		MiniCLexer lexer = new MiniCLexer(codeCharStream);
		CommonTokenStream tokens = new CommonTokenStream( lexer );
		MiniCParser parser = new MiniCParser( tokens );
		ParseTree tree = parser.program();

		ParseTreeWalker walker = new ParseTreeWalker();
		switch (getOption(args)) {
			case BYTECODEGEN:
				walker.walk(new BytecodeGenListener(), tree );
				break;
			default:
				break;
		}
	}
}
