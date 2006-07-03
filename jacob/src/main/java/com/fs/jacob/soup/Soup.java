/*
 * File:      $RCSfile$
 * Copyright: (C) 1999-2005 FiveSight Technologies Inc.
 *
 */
package com.fs.jacob.soup;

import java.io.PrintStream;

/**
 * The soup, the reactive "broth" that underlies the JACOB system. The {@link Soup}
 * implementation is responsible for implementing the JACOB reactive rules and
 * maintaining the state of the reactive broth.
 * 
 * @author Maciej Szefler <a href="mailto:mbs@fivesight.com">mbs</a>
 */
public interface Soup {

  /**
   * Are there any reactions that can be executed in the broth?
   *
   * @return <code>true</code> if there are "enabled" reactions
   */
  boolean hasReactions();

  /**
   * Add a reaction to the broth. This operation is sometimes
   * referred to as an "injection"; it can be used to inject into the
   * broth the "original" reaction.
   * @param reaction the {@link Reaction} to add to the broth
   */
  public void enqueueReaction(Reaction reaction);

  public Reaction dequeueReaction();

  public void add(CommChannel channel);

  public void add(CommGroup group);

  public String createExport(CommChannel channel);

  public CommChannel consumeExport(String exportId);

  public int cycle();

  public void flush();

  public void setClassLoader(ClassLoader classLoader);

  public void setReplacementMap(ReplacementMap replacementMap);

  public boolean isComplete();

  public void dumpState(PrintStream err);

}
