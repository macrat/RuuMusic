package jp.blanktar.ruumusic.util;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@RunWith(AndroidJUnit4.class)
public class RuuFileTest{
	Context context;
	RuuDirectory parent;
	RuuFile file;

	@Before
	public void before() throws InstantiationException, IllegalAccessException, InvocationTargetException{
		Constructor<?> ruuDirectory = RuuDirectory.class.getDeclaredConstructors()[0];
		ruuDirectory.setAccessible(true);

		context = InstrumentationRegistry.getTargetContext();
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
