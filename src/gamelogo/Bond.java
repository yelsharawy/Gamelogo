package gamelogo;
import org.nlogo.api.AnonymousCommand;
import org.nlogo.api.Context;

public abstract class Bond {
	
	public abstract void activate();
	
	public static class CommandBond extends Bond {

		Context context;
		AnonymousCommand command;
		
		public CommandBond(AnonymousCommand command, Context context) {
			this.command = command;
			this.context = context;
		}

		@Override
		public void activate() {
			org.nlogo.nvm.Context nvmContext = ((org.nlogo.nvm.ExtensionContext) context).nvmContext();
			nvmContext.finished = false;
			command.perform(new org.nlogo.nvm.ExtensionContext((org.nlogo.nvm.Workspace) context.workspace(), nvmContext), new Object[0]);
		}
		
	}
	
	public static class LambdaBond extends Bond {
		
		public static interface Lambda {
			public void run();
		}
		
		Lambda lambda;
		
		public LambdaBond(Lambda lambda) {
			this.lambda = lambda;
		}
		
		@Override
		public void activate() {
			lambda.run();
		}
	}
	
}