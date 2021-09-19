package gamelogo;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.WeakHashMap;

import org.nlogo.api.*;
import org.nlogo.app.App;
import org.nlogo.core.AgentKind;
import org.nlogo.core.ExtensionObject;
import org.nlogo.core.LogoList;
import org.nlogo.core.Reference;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.core.Token;
import org.nlogo.core.TokenType;
import org.nlogo.editor.EditorArea;
import org.nlogo.nvm.AnonymousReporter;
import org.nlogo.nvm.ExtensionContext;

import gamelogo.Bond.CommandBond;
import gamelogo.Color.DoubleColor;
import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;
import gamelogo.Bond.*;

public class GamelogoExtension extends DefaultClassManager {
	
    private static final WeakHashMap<Sprite, Object> sprites;
    private static long next;
	private static HashMap<Integer, ArrayList<Bond>> keyDownBonds = new HashMap<Integer, ArrayList<Bond>>();
	private static HashMap<Integer, ArrayList<Bond>> keyUpBonds = new HashMap<Integer, ArrayList<Bond>>();
	private static ClassManager te;

    static {
        sprites = new WeakHashMap<Sprite, Object>();
        next = 0L;
    }
    
    private static Object parseObject(String str) {
    	if (str.startsWith("\"")) {
    		return str.substring(1, str.length()-1);
    	} else if (str.startsWith("[")) {
    		if (str.startsWith("[->")) {
    			
    		} else {
    			String[] strs = str.substring(1, str.length()-1).trim().split("\\s+");
    			Object[] objs = new Object[strs.length];
    		}
    	}
    	Boolean asBool = Boolean.parseBoolean(str);
    	if (asBool || (!asBool && str.toLowerCase().equals("false"))) return asBool;
    	try {
    		return Double.parseDouble(str);
    	} catch (Exception e) {}
    	return null;
    }
    
	private static void clearBonds() {
		keyDownBonds.clear();
		keyUpBonds.clear();
	}
	
	private static Class<?> subClass(Object o, String name) {
		for (Class<?> cl : o.getClass().getDeclaredClasses()) {
			if (cl.getSimpleName().equals(name)) return cl;
		}
		return null;
	}
	
	private static Method getReport(Class<?> c) {
		try {
			return c.getMethod("report", new Class<?>[] { Argument[].class, Context.class });
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static GlobalKeyboardHook keyHook = new GlobalKeyboardHook();
	
	private static GlobalKeyListener keyListener = new GlobalKeyListener() {

		@Override
		public void keyPressed(GlobalKeyEvent arg0) {
			if (App.app().workspace().frame().isActive()) {
				Component focused = App.app().frame().getFocusOwner();
				if (App.app().tabs().getSelectedIndex() == 0 && !(focused instanceof EditorArea)) {
					//App.app().command("set testVar " + arg0.getVirtualKeyCode());
					int keyCode = arg0.getVirtualKeyCode();
					if (keyDownBonds.containsKey(keyCode)) {
						for (Bond b : keyDownBonds.get(keyCode)) {
							if (b != null) b.activate();
						}
					}
				}
			}
		}

		@Override
		public void keyReleased(GlobalKeyEvent arg0) {
			if (App.app().workspace().frame().isActive()) {
				Component focused = App.app().frame().getFocusOwner();
				if (App.app().tabs().getSelectedIndex() == 0 && !(focused instanceof EditorArea)) {
					//App.app().command("set testVar " + arg0.getVirtualKeyCode());
					int keyCode = arg0.getVirtualKeyCode();
					if (keyUpBonds.containsKey(keyCode)) {
						for (Bond b : keyUpBonds.get(keyCode)) {
							if (b != null) b.activate();
						}
					}
				}
			}
		}
		
	};
	
	private static String unescape(String str) {
		str = str.replace("\\", "\\\\");
		str = str.replace("\t", "\\t");
		str = str.replace("\b", "\\b");
		str = str.replace("\n", "\\n");
		str = str.replace("\r", "\\r");
		str = str.replace("\f", "\\f");
		str = str.replace("\"", "\\\"");
		return str;
	}
	
	private SimpleJobOwner jobOwner = new SimpleJobOwner(
			"Gamelogo",
			App.app().workspace().mainRNG(),
			AgentKind.Observer$.MODULE$);
	
	public Object eval(String str) {
		//new org.nlogo.prim.etc._runresult().report(App.app().workspace().world().observer());
		return App.app().workspace().readFromString(str);
	}
	
	public void load(PrimitiveManager primitiveManager) {
		keyHook.addKeyListener(keyListener);
		primitiveManager.addPrimitive("bindKeyDown", new BindKey(keyDownBonds));
		primitiveManager.addPrimitive("bindKeyUp", new BindKey(keyUpBonds));
		primitiveManager.addPrimitive("unbindKeyDown", new UnbindKey(keyDownBonds));
		primitiveManager.addPrimitive("unbindKeyUp", new UnbindKey(keyUpBonds));
		primitiveManager.addPrimitive("bind-var", new BindVar());
		primitiveManager.addPrimitive("create-sprite", new CreateSprite());
		primitiveManager.addPrimitive("create-clip", new CreateClip());
		primitiveManager.addPrimitive("play-clip", new PlayClip());
		primitiveManager.addPrimitive("stop-clip", new StopClip());
		primitiveManager.addPrimitive("loop-clip", new LoopClip());
		primitiveManager.addPrimitive("stop-loop", new StopLoop());
		primitiveManager.addPrimitive("draw-sprite-at", new DrawSpriteAt());
		primitiveManager.addPrimitive("unbind-all", new UnbindAll());
		primitiveManager.addPrimitive("fill-world", new FillWorld());
		primitiveManager.addPrimitive("store-level", new StoreLevel());
		primitiveManager.addPrimitive("get-raw-tileset", new GetRawTileSet());
		primitiveManager.addPrimitive("test", new TestReporter());
		te = getTableExtension();
	}
	
	public ClassManager getTableExtension() {
		ExtensionManager em = App.app().workspace().getExtensionManager();
		for (ClassManager cm : em.loadedExtensions()) {
			String name = cm.getClass().getSimpleName();
			if (name.equals("TableExtension"))
				return cm;
		}
		return null;
	}
	
	@Override
	public void unload(ExtensionManager em) throws ExtensionException {
		super.unload(em);
		keyHook.removeKeyListener(keyListener);
		keyHook.shutdownHook();
		clearBonds();
		try
	    {
	      ClassLoader classLoader = this.getClass().getClassLoader() ;
	      java.lang.reflect.Field field = ClassLoader.class.getDeclaredField( "nativeLibraries" ) ;
	      field.setAccessible(true);
	      @SuppressWarnings("unchecked")
	      Vector<Object> libs = (Vector<Object>) (field.get(classLoader)) ;
	      for ( Object o : libs )
	      {
	        java.lang.reflect.Method finalize = o.getClass().getDeclaredMethod("finalize") ;
	        finalize.setAccessible(true);
	        finalize.invoke(o);
	      }
	    }
	    catch( Exception e )
	    {
	      System.err.println( e.getMessage() ) ;
	    }
		if (currentLoop != null) currentLoop.stop();
	}
	
    private Sprite getOrCreateSpriteFromId(final long id) {
        for (final Sprite sprite : sprites.keySet()) {
            if (sprite.id == id) {
                return sprite;
            }
        }
        return new Sprite(id);
    }
    
    private static Clip currentLoop = null;
    
    public static class Clip implements ExtensionObject {

		private final Object hashKey;
		private AudioClip clip;
		
		public Clip(String fileName) throws ExtensionException {
			URL url = getURL(fileName);
			clip = Applet.newAudioClip(url);
		}
		
		{
			hashKey = new Object();
		}
		
		public void play() {
			clip.play();
		}
		
		public void stop() {
			clip.stop();
		}
		
		public void loop() {
			if (currentLoop != null) currentLoop.stop();
			clip.loop();
			currentLoop = this;
		}
		
		@Override
		public String dump(boolean readable, boolean exporting, boolean reference) {
			return "audio";
		}

		@Override
		public String getExtensionName() {
			return "gamelogo";
		}

		@Override
		public String getNLTypeName() {
			return "clip";
		}
		
		@Override
		public int hashCode() {
			return hashKey.hashCode();
		}

		@Override
		public boolean recursivelyEqual(Object obj) {
			return this == obj;
		}
    	
    }
	
	public static class Sprite implements ExtensionObject {

		private long id;
		private Color[] palette;
		private int[][] image;
		private int imageHeight;
		private int imageWidth;
		private int pivotX;
		private int pivotY;
		private final Object hashKey;
		
		@Override
		public int hashCode() {
			return hashKey.hashCode();
		}
		
		{
			hashKey = new Object();
            sprites.put(this, null);
			palette = new Color[] {
					new DoubleColor(0),
					new DoubleColor(9.9),
					new DoubleColor(15)
			};
			image = new int[][] {
				{2, 0, 1, 0, 2},
				{0, 2, 1, 2, 0},
				{1, 1, 2, 1, 1},
				{0, 2, 1, 2, 0},
				{2, 0, 1, 0, 2}
			};
			imageHeight = 5;
			imageWidth = 5;
			pivotX = 0;
			pivotY = 0;
		}
		
		Sprite(final long id) {
            this.id = id;
            next = StrictMath.max(next, id + 1L);
        }
		
		Sprite() {
			id = next++;
			sprites.put(this, null);
		}
		
		@Override
		public String dump(boolean readable, boolean exporting, boolean reference) {
			StringBuilder buf = new StringBuilder();
            if (exporting) {
                buf.append(this.id);
                if (!reference) {
                    buf.append(": ");
                }
            }
            if (!reference || !exporting) {
            	buf.append('[');
            	if (palette.length > 0) buf.append(palette[0]);
            	for (int i = 1; i < palette.length; i++) {
            		buf.append(' ');
            		buf.append(palette[i]);
            	}
            	buf.append("] ");
            	buf.append(Arrays.deepToString(image).replaceAll(",", ""));
            }
			return buf.toString();
		}

		@Override
		public String getExtensionName() {
			return "gamelogo";
		}

		@Override
		public String getNLTypeName() {
			return "sprite";
		}

		@Override
		public boolean recursivelyEqual(Object obj) {
			return this == obj;
		}

	}
	
	public static void show(String str) {
		App.app().commandLater("show \"" + unescape(str) + "\"");
	}
	

	public static void show(int n) {
		App.app().commandLater("show \"" + n + "\"");
	}
	
	public static void print(String str) {
		App.app().commandLater("print \"" + unescape(str) + "\"");
	}
	
	public static void print(int n) {
		App.app().commandLater("print \"" + n + "\"");
	}
	
	public class GetRawTileSet implements Reporter {
		
		@Override
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[] { Syntax.StringType() }, Syntax.WildcardType() );
		}

		@Override
		public Object report(Argument[] args, Context context) throws ExtensionException {
			String fileName = args[0].getString();
			File file = getFile(fileName);
			Scanner scanner = getScanner(file);
			int nColors = scanner.nextInt(); scanner.nextLine();
			for (int i = 0; i < nColors; i++) { scanner.nextLine(); }
			int nTiles = scanner.nextInt(); scanner.nextLine();
			LogoList[] tiles = new LogoList[nTiles];
			for (int i = 0; i < nTiles; i++) {
				String[] line = scanner.nextLine().split("\\s+");
				Object[] values = new Object[line.length];
				values[0] = line[0];
				for (int j = 1; j < line.length; j++) {
					values[j] = context.workspace().readFromString(line[j]);
				}
				tiles[i] = fromValues(values);
			}
			LogoList tileList = fromValues((Object[])tiles);
			scanner.close();
			return tileList;
		}
		
	}
	
	public class StoreLevel implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] { Syntax.WildcardType(), Syntax.StringType() } );
		}
		
		public LinkedHashMap<Object, Object> storeLevelFromFile(LinkedHashMap<Object, Object> obj, String fileName) {
			return obj;
		}
		
		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			LinkedHashMap<Object, Object> obj = (LinkedHashMap<Object, Object>) args[0].get();
			String fileName = args[1].getString();
			File file = getFile(fileName);
			Scanner scanner = getScanner(file);
			int nColors = scanner.nextInt(); scanner.nextLine();
			Object[] colors = new Object[nColors];
			for (int i = 0; i < nColors; i++) {
				//colors[i] = Color.parseColor(scanner.nextLine()).getValue();
				colors[i] = context.workspace().readFromString(scanner.nextLine());
			}
			LogoList colorList = fromValues(colors);
			obj.put("palette", colorList);
			int nTiles = scanner.nextInt(); scanner.nextLine();
			LogoList[] tiles = new LogoList[nTiles];
			for (int i = 0; i < nTiles; i++) {
				String[] line = scanner.nextLine().split("\\s+");
				Object[] values = new Object[line.length];
				values[0] = CreateSprite.createFromFileName(line[0]);
				for (int j = 1; j < line.length; j++) {
					values[j] = context.workspace().readFromString(line[j]);
				}
				tiles[i] = fromValues(values);
			}
			LogoList tileList = fromValues((Object[])tiles);
			obj.put("tiles", tileList);
			int height = scanner.nextInt();
			obj.put("height", Double.valueOf(height));
			int width = scanner.nextInt();
			obj.put("width", Double.valueOf(width));
			scanner.nextLine();
			LogoList[] map = new LogoList[height];
			for (int y = 0; y < height; y++) {
				Double[] row = new Double[width];
				for (int x = 0; x < width; x++) {
					row[x] = scanner.nextDouble();
				}
				if (y < height - 1) scanner.nextLine();
				LogoList rowList = fromValues((Object[])row);
				map[height-y-1] = rowList;
			}
			LogoList mapList = fromValues((Object[])map);
			obj.put("map", mapList);
			scanner.close();
		}
		
	}
	
	public static LogoList fromValues(Object... objs) {
		return LogoList.fromJava(Arrays.asList(objs));
	}
	
	public static Scanner getScanner(File file) throws ExtensionException {
		try {
			return new Scanner(file);
		} catch (FileNotFoundException e) {
			throw new ExtensionException("file "+file.getPath()+" not found");
		}
	}
	
	public static File getFile(String input) throws ExtensionException {
		String path;
		try {
			path = App.app().workspace().attachModelDir(input);
		} catch (MalformedURLException e) {
			throw new ExtensionException("file "+input+" not found");
		}
		File file = new File(path);
		return file;
	}

	public static URL getURL(String input) throws ExtensionException {
		try {
			return getFile(input).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new ExtensionException("file "+input+" not found");
		}	
	}
	
	public static class CreateSprite implements Reporter {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[] { Syntax.StringType() }, Syntax.WildcardType());
		}
		
		public static Sprite createFromFileName(String input) throws ExtensionException {
			File file = getFile(input);
			Sprite sprite = new Sprite();
			try {
				Scanner scanner = new Scanner(file);
				int palleteLength = scanner.nextInt();
				scanner.nextLine();
				Color[] pallete = new Color[palleteLength];
				for (int i = 0; i < palleteLength; i++) {
					pallete[i] = Color.parseColor(scanner.nextLine());
				}
				int height = scanner.nextInt();
				int width = scanner.nextInt();
				scanner.nextLine();
				int[][] image = new int[height][width];
				for (int y = 0; y < height; y++) {
					String[] line = scanner.nextLine().split(" ");
					for (int x = 0; x < width; x++) {
						image[y][x] = Integer.parseInt(line[x].trim());
					}
				}
				sprite.palette = pallete;
				sprite.imageHeight = height;
				sprite.imageWidth = width;
				sprite.image = image;
				if (scanner.hasNextInt()) {
					sprite.pivotY = scanner.nextInt();
					sprite.pivotX = scanner.nextInt();
				}
				scanner.close();
			} catch (FileNotFoundException e) {
				throw new ExtensionException("file " + input + " does not exist relative to the model");
			} catch (Exception e) {
				throw new ExtensionException("file " + input + " is not in the correct format " + e.toString());
			}
			return sprite;
		}
		
		@Override
		public Object report(Argument[] args, Context context) throws ExtensionException {
			String input = args[0].getString();
			return createFromFileName(input);
		}

	}
	
	public class DrawSpriteAt implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] { Syntax.ListType(), Syntax.NumberType(), Syntax.NumberType(), Syntax.WildcardType(), Syntax.BooleanType() });
		}
		
        public String getAgentClassString() {
            return "O";
        }
        
		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			Sprite sprite = (Sprite)args[3].get();
			boolean flipped = args[4].getBooleanValue();
			int sx = args[1].getIntValue();
			//if (flipped) sx -= sprite.imageWidth - 1 - sprite.pivotX; else sx -= sprite.pivotX;
			sx -= sprite.pivotX;
			int sy = args[2].getIntValue() + sprite.pivotY;
			World world = context.world();
			int yMin = StrictMath.max(0, sy - world.maxPycor());
			int xMin = StrictMath.max(0, world.minPxcor() - sx);			
			int yMax = StrictMath.min(sprite.imageHeight, sy - world.minPycor() + 1);
			int xMax = StrictMath.min(sprite.imageWidth, world.maxPxcor() - sx + 1);
			Color[] palette = sprite.palette;
			int paletteSize = palette.length;
			LogoList worldPalette = args[0].getList();
			for (int y = yMin; y < yMax; y++) {
				for (int x = xMin; x < xMax; x++) {
					int paletteIndex = sprite.image[y][flipped? sprite.imageWidth - 1 - x : x]-1;
					if (paletteIndex != -1) {
						Object c; 
						try {
							c = paletteIndex < paletteSize ? palette[paletteIndex].getValue() :
							worldPalette.get(paletteIndex - paletteSize+1);
						} catch (Exception e) {
							c = new Color.ListColor(255, 0, 255).value;
						}
						try {
							world.fastGetPatchAt(sx+x, sy-y).setVariable(2, c);
						} catch (LogoException | AgentException e) {
							throw new ExtensionException("error drawing sprite: " + e.toString());
						}
					}
				}
			}
		}
	}
	
	public class UnbindAll implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax();
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			clearBonds();
		}
	}
	public class FillWorld implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] {Syntax.NumberType() | Syntax.ListType()});
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			for (Agent p : context.world().patches().agents()) {
				try {
					p.setVariable(2, args[0].get());
				} catch (Exception e) {
					throw new ExtensionException("error filling world: " + e.getMessage());
				}
			}
		}
	}
	
	public class BindVar implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] { Syntax.NumberType(), Syntax.ReferenceType()} );
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			Reference t = (Reference) args[1].get();
			int vn = t.vn();
			int keyCode = args[0].getIntValue();
			try {
				context.world().observer().setVariable(vn, false);
				Bond bondDown = new LambdaBond(() -> {
					try {
						context.world().observer().setVariable(vn, true);
					} catch (Exception e) {
						
					}
				});
				Bond bondUp = new LambdaBond(() -> {
					try {
						context.world().observer().setVariable(vn, false);
					} catch (Exception e) {
						
					}
				});
				if (keyDownBonds.containsKey(keyCode))
					keyDownBonds.get(keyCode).add(bondDown);
				else {
					ArrayList<Bond> list = new ArrayList<Bond>();
					list.add(bondDown);
					keyDownBonds.put(keyCode, list);
				}
				if (keyUpBonds.containsKey(keyCode))
					keyUpBonds.get(keyCode).add(bondUp);
				else {
					ArrayList<Bond> list = new ArrayList<Bond>();
					list.add(bondUp);
					keyUpBonds.put(keyCode, list);
				}
			} catch (Exception e) {
				throw new ExtensionException("error " + e.getMessage());
			}
		}
		
	}
	public class TestReporter implements Reporter {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[] {Syntax.ReferenceType()}, Syntax.StringType());
		}

		@Override
		public Object report(Argument[] args, Context context) throws ExtensionException {
			Reference t = (Reference) args[0].get();
			return Integer.toString(t.vn());
		}
		
	}
	public class CreateClip implements Reporter {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.reporterSyntax(new int[] {Syntax.StringType()}, Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] args, Context context) throws ExtensionException {
			return new Clip(args[0].getString());
		}
		
	}
	public class PlayClip implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] {Syntax.WildcardType()});
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			Clip c = ((Clip)args[0].get());
			c.stop();
			c.play();
		}
	}
	public class StopClip implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] {Syntax.WildcardType()});
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			((Clip)args[0].get()).stop();
		}
	}
	public class LoopClip implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[] {Syntax.WildcardType()});
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			((Clip)args[0].get()).loop();
		}
	}
	public class StopLoop implements Command {

		@Override
		public Syntax getSyntax() {
			return SyntaxJ.commandSyntax(new int[0]);
		}

		@Override
		public void perform(Argument[] args, Context context) throws ExtensionException {
			if (currentLoop != null) currentLoop.stop();
		}
	}
}