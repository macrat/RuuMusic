package jp.blanktar.ruumusic.service;

import jp.blanktar.ruumusic.test.TestBase;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.NoSuchFieldException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


public class PlaylistTest extends TestBase{
	@Before
	public void setUpDirectories() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException{
		Constructor<?> ruuDirectory = RuuDirectory.class.getDeclaredConstructors()[0];
		ruuDirectory.setAccessible(true);

		Field root = RuuDirectory.class.getDeclaredField("root");
		root.setAccessible(true);
		root.set(null, ruuDirectory.newInstance(context, null, "/", new String[]{
			"/path/to/0-music.mp3",
			"/path/to/1-test.mp3",
			"/path/to/2-hoge.mp3",
			"/path/to/3-fuga.mp3",
			"/path/0-foo.mp3",
			"/path/1-bar.mp3",
		}));
		root.setAccessible(false);
	}

	@Test
	public void getByMusicPath() throws RuuFileBase.NotFound, Playlist.EmptyDirectory{
		Playlist playlist = Playlist.getByMusicPath(context, "/path/to/0-music");
		assertNotNull(playlist);
		assertEquals(playlist.type, Playlist.Type.SIMPLE);
		assertEquals(playlist.path.getFullPath(), "/path/to/");
		assertEquals(playlist.query, null);
		assertEquals(playlist.getCurrent().getName(), "0-music");

		playlist.goLast();
		assertEquals(playlist.getCurrent().getFullPath(), "/path/to/3-fuga");
	}

	@Test
	public void getRecurcive() throws RuuFileBase.NotFound, Playlist.EmptyDirectory{
		Playlist playlist = Playlist.getRecursive(context, "/path/");
		assertNotNull(playlist);
		assertEquals(playlist.type, Playlist.Type.RECURSIVE);
		assertEquals(playlist.path.getFullPath(), "/path/");
		assertEquals(playlist.query, null);
		assertEquals(playlist.getCurrent().getName(), "0-music");

		playlist.goLast();
		assertEquals(playlist.getCurrent().getFullPath(), "/path/1-bar");
	}

	@Test
	public void getSearchResults() throws RuuFileBase.NotFound, Playlist.EmptyDirectory{
		Playlist playlist = Playlist.getSearchResults(context, "/path/", "1-");
		assertNotNull(playlist);
		assertEquals(playlist.type, Playlist.Type.SEARCH);
		assertEquals(playlist.path.getFullPath(), "/path/");
		assertEquals(playlist.query, "1-");
		assertEquals(playlist.getCurrent().getName(), "1-test");

		playlist.goLast();
		assertEquals(playlist.getCurrent().getFullPath(), "/path/1-bar");
	}

	@Test(expected = Playlist.EmptyDirectory.class)
	public void getSearchResults_EmptyDirectory() throws RuuFileBase.NotFound, Playlist.EmptyDirectory{
		Playlist.getSearchResults(context, "/path/", "not found");
	}

	@Test
	public void move() throws RuuFileBase.NotFound, Playlist.EmptyDirectory, Playlist.NotFound, Playlist.EndOfList{
		Playlist playlist = Playlist.getByMusicPath(context, "/path/to/0-music");
		RuuFile first = playlist.getCurrent();
		assertEquals(first.getName(), "0-music");

		playlist.goLast();
		assertEquals(playlist.getCurrent().getName(), "3-fuga");

		try{
			playlist.goNext();
			fail("Playlist.EndOfList is expected");
		}catch(Playlist.EndOfList e){
		}

		playlist.goPrev();
		assertEquals(playlist.getCurrent().getName(), "2-hoge");

		playlist.goMusic(first);
		assertEquals(playlist.getCurrent().getName(), "0-music");

		playlist.goNext();
		assertEquals(playlist.getCurrent().getName(), "1-test");

		playlist.goFirst();
		assertEquals(playlist.getCurrent().getName(), "0-music");

		try{
			playlist.goPrev();
			fail("Playlist.EndOfList is expected");
		}catch(Playlist.EndOfList e){
		}
	}

	@Test
	public void sort_shuffle() throws RuuFileBase.NotFound, Playlist.EmptyDirectory, Playlist.EndOfList{
		Playlist playlist = Playlist.getByMusicPath(context, "/path/to/0-music");
		assertEquals(playlist.getCurrent().getName(), "0-music");

		for(int i=0;; i++){
			playlist.shuffle(false);
			if(!playlist.getCurrent().getName().equals("0-music")){
				break;
			}
			if(i > 10){
				fail("didn't shuffle");
			}
		}

		playlist.sort();
		playlist.goFirst();
		assertEquals(playlist.getCurrent().getName(), "0-music");
		playlist.goNext();
		assertEquals(playlist.getCurrent().getName(), "1-test");
		playlist.goNext();
		assertEquals(playlist.getCurrent().getName(), "2-hoge");
		playlist.goNext();
		assertEquals(playlist.getCurrent().getName(), "3-fuga");
		playlist.goFirst();
		assertEquals(playlist.getCurrent().getName(), "0-music");

		for(int i=0; i<10; i++){
			playlist.shuffle(true);
			assertEquals(playlist.getCurrent().getName(), "0-music");
			playlist.goNext();
			assertFalse(playlist.getCurrent().getName().equals("0-music"));
			playlist.goPrev();
			assertEquals(playlist.getCurrent().getName(), "0-music");
		}
	}
}
