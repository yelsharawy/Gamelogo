package gamelogo;
import java.util.ArrayList;
import java.util.HashMap;

import org.nlogo.api.*;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

public class UnbindKey implements Command {
	
	private HashMap<Integer, ArrayList<Bond>> keyBonds;
	
	public UnbindKey (HashMap<Integer, ArrayList<Bond>> keyBonds) {
		super();
		this.keyBonds = keyBonds;
	}
	
	@Override
	public Syntax getSyntax() {
		return SyntaxJ.commandSyntax(new int[] { Syntax.NumberType() });
	}

	@Override
	public void perform(Argument[] args, Context context) throws ExtensionException {
		int keyCode = args[0].getIntValue();
		if (keyBonds.containsKey(keyCode))
			keyBonds.remove(keyCode);
	}

}
