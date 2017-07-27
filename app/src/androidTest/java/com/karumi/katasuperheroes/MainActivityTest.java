/*
 * Copyright (C) 2015 Karumi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.karumi.katasuperheroes;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.matcher.IntentMatchers;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.karumi.katasuperheroes.di.MainComponent;
import com.karumi.katasuperheroes.di.MainModule;
import com.karumi.katasuperheroes.matchers.RecyclerViewItemsCountMatcher;
import com.karumi.katasuperheroes.model.SuperHero;
import com.karumi.katasuperheroes.model.SuperHeroesRepository;
import com.karumi.katasuperheroes.recyclerview.RecyclerViewInteraction;
import com.karumi.katasuperheroes.ui.view.MainActivity;
import com.karumi.katasuperheroes.ui.view.SuperHeroDetailActivity;

import it.cosenonjaviste.daggermock.DaggerMockRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class) @LargeTest public class MainActivityTest {

  @Rule public DaggerMockRule<MainComponent> daggerRule =
      new DaggerMockRule<>(MainComponent.class, new MainModule()).set(
          new DaggerMockRule.ComponentSetter<MainComponent>() {
            @Override public void setComponent(MainComponent component) {
              SuperHeroesApplication app =
                  (SuperHeroesApplication) InstrumentationRegistry.getInstrumentation()
                      .getTargetContext()
                      .getApplicationContext();
              app.setComponent(component);
            }
          });

  @Rule public IntentsTestRule<MainActivity> activityRule =
      new IntentsTestRule<>(MainActivity.class, true, false);

  @Mock SuperHeroesRepository repository;

  @Test public void showsEmptyCaseIfThereAreNoSuperHeroes() {
    givenThereAreNoSuperHeroes();

    startActivity();

    onView(withText("¯\\_(ツ)_/¯")).check(matches(isDisplayed()));
  }

  @Test public void doesNotShowEmptyCaseIfThereAreSuperHeroes() {
    givenThereAreSuperHeroes();

    startActivity();

    onView(withText("¯\\_(ツ)_/¯")).check(matches(not(isDisplayed())));
  }

  @Test public void doesNotShowSpinnerIfSuperHeroesHaveBeenFetched() {
    givenThereAreSuperHeroes();

    startActivity();

    onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())));
  }

  @Test public void herosAreShownInProperOrderInTheList() {
    List<SuperHero> heroes = givenThereAreSuperHeroes();

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
    .withItems(heroes)
    .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
        @Override
        public void check(SuperHero item, View view, NoMatchingViewException e) {
          matches(hasDescendant(withText(item.getName()))).check(view, e);
        }
    });

  }

  @Test public void avengersHerosAreShownWithThe_A_Badge() {
    List<SuperHero> heroes = givenThereAreSuperHeroes();

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
            .withItems(heroes)
            .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
              @Override
              public void check(SuperHero hero, View view, NoMatchingViewException e) {
                if (hero.isAvenger()) {
                  matches(hasDescendant(allOf(withId(R.id.iv_avengers_badge), isDisplayed()))).check(view, e);
                }
              }
            });
  }

  @Test public void checkThatDetailsAreOpenedWhenClickingOnARow() {
    List<SuperHero> heroes = givenThereAreSuperHeroes();

    startActivity();

    onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition(0, ViewActions.click()));


    SuperHero selectedHero = heroes.get(0);
    Intents.intended(IntentMatchers.hasComponent(SuperHeroDetailActivity.class.getCanonicalName()));
    Intents.intended(IntentMatchers.hasExtra("super_hero_name_key", selectedHero.getName()));
  }


  @Test public void noAvengersHerosAreShownWithoutThe_A_Badge() {
    List<SuperHero> heroes = givenThereAreSuperHeroes();

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
            .withItems(heroes)
            .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
              @Override
              public void check(SuperHero hero, View view, NoMatchingViewException e) {
                if (!hero.isAvenger()) {
                  matches(hasDescendant(allOf(withId(R.id.iv_avengers_badge), not(isDisplayed())))).check(view, e);
                }
              }
            });
  }



  private void givenThereAreNoSuperHeroes() {
    when(repository.getAll()).thenReturn(Collections.<SuperHero>emptyList());
  }

  private List<SuperHero> givenThereAreSuperHeroes() {
    List<SuperHero> heroes = new ArrayList<>();
    for (int i = 0; i<10; i++) {
      SuperHero hero = new SuperHero("Superlopez " + i,
              "http://http://nadiaorenes.es/blog/wp-content/uploads/2014/12/superlopez-271x300.jpg",
              i % 2 == 0 ? true: false,
              "Super López " + i);
      heroes.add(hero);

      when(repository.getByName(hero.getName())).thenReturn(hero);
    }

    when(repository.getAll()).thenReturn(heroes);
    return heroes;
  }

  private MainActivity startActivity() {
    return activityRule.launchActivity(null);
  }
}