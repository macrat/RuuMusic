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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;


@RunWith(AndroidJUnit4.class)
public class RuuDirectoryTest{
	Context context;
	RuuDirectory parent, dir;

	@Before
	public void before() throws InstantiationException, IllegalAccessException, InvocationTargetException{
		Constructor<?> ruuDirectory = RuuDirectory.class.getDeclaredConstructors()[0];
		ruuDirectory.setAccessible(true);

		context = InstrumentationRegistry.getTargetContext();
		parent = (RuuDirectory)ruuDirectory.newInstance(context, null, "/", null);
		dir = (RuuDirectory)ruuDirectory.newInstance(context, parent, "/hoge", new String[]{"/hoge/dir/music.mp3", "/hoge/child.mp3"});
	}

	@Test
	public void isDirectory(){
		assertTrue(dir.isDirectory());
	}

	@Test
	public void getFullPath(){
		assertEquals(dir.getFullPath(), "/hoge/");
	}

	@Test
	public void getRuuPath() throws RuuFileBase.OutOfRootDirectory{
		Preference.Str.ROOT_DIRECTORY.set(context, "/hoge/");
		assertEquals(dir.getRuuPath(), "/");
	}

	@Test(expected = RuuFileBase.OutOfRootDirectory.class)
	public void outOfRoot() throws RuuFileBase.OutOfRootDirectory{
		Preference.Str.ROOT_DIRECTORY.set(context, "/out/");
		dir.getRuuPath();
	}

	@Test
	public void getName(){
		assertEquals(dir.getName(), "hoge");
	}

	@Test
	public void getParent() throws RuuFileBase.OutOfRootDirectory{
		assertEquals(dir.getParent(), parent);
	}

	@Test(expected = RuuFileBase.OutOfRootDirectory.class)
	public void outOfRootDirectory() throws RuuFileBase.OutOfRootDirectory{
		parent.getParent();
	}

	@Test
	public void contains(){
		assertTrue(parent.contains(dir));
		assertFalse(dir.contains(parent));
	}

	@Test
	public void getDirectories(){
		List<RuuDirectory> dirs = dir.getDirectories();

		assertEquals(dirs.size(), 1);
		assertEquals(dirs.get(0).getName(), "dir");
		assertEquals(dirs.get(0).getFullPath(), "/hoge/dir/");
	}

	@Test
	public void getMusics(){
		List<RuuFile> musics = dir.getMusics();

		assertEquals(musics.size(), 1);
		assertEquals(musics.get(0).getName(), "child");
		assertEquals(musics.get(0).getFullPath(), "/hoge/child");
		assertEquals(musics.get(0).getRealPath(), "/hoge/child.mp3");
	}

	@Test
	public void getMusicsRecursive(){
		List<RuuFile> musics = dir.getMusicsRecursive();

		assertEquals(musics.size(), 2);
		assertEquals(musics.get(0).getFullPath(), "/hoge/dir/music");
		assertEquals(musics.get(1).getFullPath(), "/hoge/child");
	}
}
