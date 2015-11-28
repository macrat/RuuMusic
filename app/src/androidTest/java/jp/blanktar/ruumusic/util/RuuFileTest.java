package jp.blanktar.ruumusic.util;

import jp.blanktar.ruumusic.test.TestBase;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class RuuFileTest extends TestBase{
	RuuDirectory parent;
	RuuFile file;

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException, InvocationTargetException{
		Constructor<?> ruuDirectory = RuuDirectory.class.getDeclaredConstructors()[0];
		ruuDirectory.setAccessible(true);

		parent = (RuuDirectory)ruuDirectory.newInstance(context, null, "/hoge/", null);
		file = new RuuFile(context, parent, "/hoge/test", new String[]{".ext"});
	}

	@Test
	public void isDirectory(){
		assertFalse(file.isDirectory());
	}

	@Test
	public void getFullPath(){
		assertEquals(file.getFullPath(), "/hoge/test");
	}

	@Test
	public void getRuuPath() throws RuuFileBase.OutOfRootDirectory{
		Preference.Str.ROOT_DIRECTORY.set(context, "/hoge/");
		assertEquals(file.getRuuPath(), "/test");
	}

	@Test(expected = RuuFileBase.OutOfRootDirectory.class)
	public void outOfRoot() throws RuuFileBase.OutOfRootDirectory{
		Preference.Str.ROOT_DIRECTORY.set(context, "/out/");
		file.getRuuPath();
	}

	@Test
	public void getRealPath(){
		assertEquals(file.getRealPath(), "/hoge/test.ext");
	}

	@Test
	public void getName(){
		assertEquals(file.getName(), "test");
	}

	@Test
	public void getParent(){
		assertEquals(file.getParent(), parent);
	}
}
