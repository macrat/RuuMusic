package jp.blanktar.ruumusic.test;

import android.support.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;


@RunWith(AndroidJUnit4.class)
public class TestBase{
	protected Context context;

	@Before
	@CallSuper
	public void setUpContext(){
		context = InstrumentationRegistry.getTargetContext();
	}
}

