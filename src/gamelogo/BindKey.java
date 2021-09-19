package gamelogo;
import java.util.ArrayList;
import java.util.HashMap;

import org.nlogo.api.*;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import gamelogo.Bond.CommandBond;

public class BindKey implements Command {
	
	private HashMap<Integer, ArrayList<Bond>> keyBonds;
	
	public BindKey (HashMap<Integer, ArrayList<Bond>> keyBonds) {
		super();
		this.keyBonds = keyBonds;
	}
	
	@Override
	public Syntax getSyntax() {
		return SyntaxJ.commandSyntax(new int[] { Syntax.NumberType(), Syntax.CommandType() });
	}

	@Override
	public void perform(Argument[] args, Context context) throws ExtensionException {
		AnonymousCommand command = args[1].getCommand();
		if (command.syntax().minimum() == 0) {
			int keyCode = args[0].getIntValue();
			Bond bond = new CommandBond(command, context);
			if (keyBonds.containsKey(keyCode))
				keyBonds.get(keyCode).add(bond);
			else {
				ArrayList<Bond> list = new ArrayList<Bond>();
				list.add(bond);
				keyBonds.put(keyCode, list);
			}
		} else {
			throw new ExtensionException("command with 0 parameters expected");
		}
	}

}
