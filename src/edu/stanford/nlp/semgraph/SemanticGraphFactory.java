package edu.stanford.nlp.semgraph;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Filter;
import edu.stanford.nlp.util.Filters;
import edu.stanford.nlp.util.Generics;

import java.util.*;

/**
 * Refactoring of static makers of SemanticGraphs in order to simplify
 * the SemanticGraph class.
 *
 * @author rafferty
 */
public class SemanticGraphFactory {

  private SemanticGraphFactory() {} // just static factory methods

  private static final boolean INCLUDE_PUNCTUATION_DEPENDENCIES = false;

  public enum Mode {
    COLLAPSED_TREE,
    /** collapse: Whether to do "collapsing" of pairs of dependencies into
     *  single dependencies, e.g., for prepositions and conjunctions.
     */
    COLLAPSED,
    /** ccProcess: Whether to do processing of CC complements resulting from
     *  collapsing.  This argument is ignored unless <code>collapse</code> is
     * <code>true</code>.
     */
    CCPROCESSED,
    BASIC
  }

  /**
   * Produces an Uncollapsed SemanticGraph with no extras.
   */
  public static SemanticGraph generateUncollapsedDependencies(Tree tree) {
    return makeFromTree(tree, Mode.BASIC, false, true);
  }

  /**
   * Produces a Collapsed SemanticGraph with no extras.
   */
  public static SemanticGraph generateCollapsedDependencies(Tree tree) {
    return makeFromTree(tree, Mode.COLLAPSED, false, true);
  }

  /**
   * Produces a CCProcessed SemanticGraph with no extras.
   */
  public static SemanticGraph generateCCProcessedDependencies(Tree tree) {
    return makeFromTree(tree, Mode.CCPROCESSED, false, true);
  }

  /**
   * Produces an Uncollapsed SemanticGraph with no extras.
   */
  public static SemanticGraph generateUncollapsedDependencies(GrammaticalStructure gs) {
    return makeFromTree(gs, Mode.BASIC, false, true, null);
  }

  /**
   * Produces a Collapsed SemanticGraph with no extras.
   */
  public static SemanticGraph generateCollapsedDependencies(GrammaticalStructure gs) {
    return makeFromTree(gs, Mode.COLLAPSED, false, true, null);
  }

  /**
   * Produces a CCProcessed SemanticGraph with no extras.
   */
  public static SemanticGraph generateCCProcessedDependencies(GrammaticalStructure gs) {
    return makeFromTree(gs, Mode.CCPROCESSED, false, true, null);
  }



  /**
   * Returns a new <code>SemanticGraph</code> constructed from a given {@link
   * Tree} with given options. <p/>
   *
   * This factory method is intended to replace a profusion of highly similar
   * factory methods, such as
   * <code>typedDependencies()</code>,
   * <code>typedDependenciesCollapsed()</code>,
   * <code>allTypedDependencies()</code>,
   * <code>allTypedDependenciesCollapsed()</code>, etc. <p/>
   *
   * For a fuller explanation of the meaning of the boolean arguments, see
   * {@link GrammaticalStructure}. <p/>
   *
   * @param tree A tree representing a phrase structure parse
   * @param includeExtras Whether to include extra dependencies, which may
   * result in a non-tree
   * @param threadSafe Whether to make sure processing is thread-safe
   * @param filter A filter to exclude certain dependencies; ignored if null
   * @return A SemanticGraph
   */
  public static SemanticGraph makeFromTree(Tree tree,
                                           Mode mode,
                                           boolean includeExtras,
                                           boolean threadSafe,
                                           Filter<TypedDependency> filter) {
    Filter<String> wordFilt;
    if (INCLUDE_PUNCTUATION_DEPENDENCIES) {
      wordFilt = Filters.acceptFilter();
    } else {
      wordFilt = new PennTreebankLanguagePack().punctuationWordRejectFilter();
    }
    GrammaticalStructure gs = new EnglishGrammaticalStructure(tree,
            wordFilt,
            new SemanticHeadFinder(true),
            threadSafe);
    return makeFromTree(gs, mode, includeExtras,
                        threadSafe, filter);
  }


  // TODO: these booleans would be more readable as enums similar to Mode.
  // Then the arguments would make more sense
  public static SemanticGraph makeFromTree(GrammaticalStructure gs,
                                           Mode mode,
                                           boolean includeExtras,
                                           boolean threadSafe,
                                           Filter<TypedDependency> filter) {
    addProjectedCategoriesToGrammaticalStructure(gs);
    Collection<TypedDependency> deps;
    switch(mode) {
    case COLLAPSED_TREE:
      deps = gs.typedDependenciesCollapsedTree();
      break;
    case COLLAPSED:
      deps = gs.typedDependenciesCollapsed(includeExtras);
      break;
    case CCPROCESSED:
      deps = gs.typedDependenciesCCprocessed(includeExtras);
      break;
    case BASIC:
      deps = gs.typedDependencies(includeExtras);
      break;
    default:
      throw new IllegalArgumentException("Unknown mode " + mode);
    }

    if (filter != null) {
      List<TypedDependency> depsFiltered = Generics.newArrayList();
      for (TypedDependency td : deps) {
        if (filter.accept(td)) {
          depsFiltered.add(td);
        }
      }
      deps = depsFiltered;
    }

    // there used to be an if clause that filtered out the case of empty
    // dependencies. However, I could not understand (or replicate) the error
    // it alluded to, and it led to empty dependency graphs for very short fragments,
    // which meant they were ignored by the RTE system. Changed. (pado)
    // See also the SemanticGraph constructor.

    //System.err.println(deps.toString());
    return new SemanticGraph(deps);
  }


  public static SemanticGraph makeFromTree(GrammaticalStructure structure) {
    return makeFromTree(structure, Mode.BASIC, false, false, null);
  }


  public static SemanticGraph makeFromTree(Tree tree,
                                           Mode mode,
                                           boolean includeExtras,
                                           Filter<TypedDependency> filter) {
    return makeFromTree(tree, mode, includeExtras, false, filter);
  }


  public static SemanticGraph makeFromTree(Tree tree,
                                           Mode mode,
                                           boolean includeExtras,
                                           boolean threadSafe) {
    return makeFromTree(tree, mode, includeExtras, threadSafe, null);
  }

  /**
   * Returns a new SemanticGraph constructed from the given tree.  Dependencies are collapsed
   * according to the parameter "collapse", and extra dependencies are not included
   * @param tree tree from which to make new semantic graph
   * @param collapse collapse dependencies iff this parameter is true
   */
  public static SemanticGraph makeFromTree(Tree tree, boolean collapse) {
    return makeFromTree(tree, (collapse) ? Mode.COLLAPSED : Mode.BASIC, false, false, null);
  }

  /**
   * Returns a new SemanticGraph constructed from the given tree.  Dependencies are collapsed,
   * and extra dependencies are not included (convenience method for makeFromTree(Tree tree, boolean collapse))
   */
  public static SemanticGraph makeFromTree(Tree tree) {
    return makeFromTree(tree, Mode.COLLAPSED, false, false, null);
  }


  /**
   * Returns a new SemanticGraph constructed from the given tree. Collapsing
   * of dependencies is performed according to "collapse". The list includes extra
   * dependencies which do not respect a tree structure of the
   * dependencies. <p/>
   *
   * (Internally, this invokes (@link
   * edu.stanford.nlp.trees.GrammaticalStructure#allTypedDependencies()
   * GrammaticalStructure.allTypedDependencies()).)
   *
   * @param tree tree from which to make new semantic graph
   * @param collapse collapse dependencies iff this parameter is true
   */
  // todo: Should we now update this to do CC process by default?
  public static SemanticGraph allTypedDependencies(Tree tree, boolean collapse) {
    return makeFromTree(tree, (collapse) ? Mode.COLLAPSED : Mode.BASIC, true, null);
  }

  /**
   * Modifies the given GrammaticalStructure by adding some annotations to the
   * MapLabels of certain nodes. <p/>
   *
   * For each word (leaf node), we add an annotation which indicates the
   * syntactic category of the maximal constituent headed by the word.
   */
  static void addProjectedCategoriesToGrammaticalStructure(GrammaticalStructure gs) {
    // Our strategy: (1) assume every node in GrammaticalStructure is already
    // annotated with head word, (2) traverse nodes of GrammaticalStructure in
    // reverse of pre-order (bottom up), and (3) at each, get head word and
    // annotate it with category of this node.
    List<TreeGraphNode> nodes = new ArrayList<TreeGraphNode>();
    for (Tree node : gs.root()) {       // pre-order traversal
      nodes.add((TreeGraphNode) node);
    }
    Collections.reverse(nodes);         // reverse
    for (TreeGraphNode node : nodes) {
      if (!"ROOT".equals(node.value())) { // main verb should get PROJ_CAT "S", not "ROOT"
        CoreLabel label = node.label();
        Tree hw = label.get(TreeCoreAnnotations.HeadWordAnnotation.class);
        if (hw != null) {
          TreeGraphNode hwn = (TreeGraphNode) hw;
          CoreLabel hwLabel = hwn.label();
          hwLabel.set(CoreAnnotations.ProjectedCategoryAnnotation.class, node.value());
        }
      }
    }
  }

  /**
   * Given a list of edges, attempts to create and return a rooted SemanticGraph.
   * <p>
   * TODO: throw Exceptions, or flag warnings on conditions for concern (no root, etc)
   */
  public static SemanticGraph makeFromEdges(Iterable<SemanticGraphEdge> edges) {
    // Identify the root(s) of this graph
    SemanticGraph sg = new SemanticGraph();
    Collection<IndexedWord> vertices = getVerticesFromEdgeSet(edges);
    for (IndexedWord vertex : vertices) {
      sg.addVertex(vertex);
    }
    for (SemanticGraphEdge edge : edges) {
      sg.addEdge(edge.getSource(),edge.getTarget(), edge.getRelation(), edge.getWeight(), edge.isExtra());
    }

    sg.resetRoots();
    return sg;
  }

  /**
   * Given an iterable set of edges, returns the set of  vertices covered by these edges.
   * <p>
   * Note: CDM changed the return of this from a List to a Set in 2011. This seemed more
   * sensible.  Hopefully it doesn't break anything....
   */
  public static Set<IndexedWord> getVerticesFromEdgeSet(Iterable<SemanticGraphEdge> edges) {
    Set<IndexedWord> retSet = Generics.newHashSet();
    for (SemanticGraphEdge edge : edges) {
      retSet.add(edge.getGovernor());
      retSet.add(edge.getDependent());
    }
    return retSet;
  }


  /**
   * Given a set of vertices, and the source graph they are drawn from, create a path composed
   * of the minimum paths between the vertices.  i.e. this is a simple brain-dead attempt at getting
   * something approximating a minimum spanning graph.
   *
   * NOTE: the hope is the vertices will already be contiguous, but facilities are added just in case for
   * adding additional nodes.
   */
  public static SemanticGraph makeFromVertices(SemanticGraph sg, Collection<IndexedWord> nodes) {
    List<SemanticGraphEdge> edgesToAdd = new ArrayList<SemanticGraphEdge>();
    List<IndexedWord> nodesToAdd = new ArrayList<IndexedWord>(nodes);
    for (IndexedWord nodeA :nodes) {
      for (IndexedWord nodeB : nodes) {
        if (nodeA != nodeB) {
          List<SemanticGraphEdge> edges = sg.getShortestDirectedPathEdges(nodeA, nodeB);
          if (edges != null) {
            edgesToAdd.addAll(edges);
            for (SemanticGraphEdge edge : edges) {
              IndexedWord gov = edge.getGovernor();
              IndexedWord dep = edge.getDependent();
              if (gov != null && !nodesToAdd.contains(gov)) {
                nodesToAdd.add(gov);
              }
              if (dep != null && !nodesToAdd.contains(dep)) {
                nodesToAdd.add(dep);
              }
            }
          }
        }
      }
    }

    SemanticGraph retSg = new SemanticGraph();
    for (IndexedWord node : nodesToAdd) {
      retSg.addVertex(node);
    }
    for (SemanticGraphEdge edge : edgesToAdd) {
      retSg.addEdge(edge.getGovernor(), edge.getDependent(), edge.getRelation(), edge.getWeight(), edge.isExtra());
    }

    retSg.resetRoots();
    return retSg;
  }

  /**
   * This creates a new graph based off the given, but uses the existing nodes objects.
   */
  public static SemanticGraph duplicateKeepNodes(SemanticGraph sg) {
    SemanticGraph retSg = new SemanticGraph();
    for (IndexedWord node : sg.vertexSet()) {
      retSg.addVertex(node);
    }
    retSg.setRoots(sg.getRoots());
    for (SemanticGraphEdge edge : sg.edgeIterable()) {
      retSg.addEdge(edge.getGovernor(), edge.getDependent(), edge.getRelation(), edge.getWeight(), edge.isExtra());
    }
    return retSg;
  }

  /**
   * Given a list of graphs, constructs a new graph combined from the
   * collection of graphs.  Original vertices are used, edges are
   * copied.  Graphs are ordered by the sentence index and index of
   * the original vertices.  Intent is to create a "mega graph"
   * similar to the graphs used in the RTE problem.
   * <br>
   * This method only works if the indexed words have different
   * sentence ids, as otherwise the maps used will confuse several of
   * the IndexedWords.
   */
  public static SemanticGraph makeFromGraphs(Collection<SemanticGraph> sgList) {
    SemanticGraph sg = new SemanticGraph();
    Collection<IndexedWord> newRoots = Generics.newHashSet();
    for (SemanticGraph currSg : sgList) {
      newRoots.addAll(currSg.getRoots());
      for (IndexedWord currVertex : currSg.vertexSet())
        sg.addVertex(currVertex);
      for (SemanticGraphEdge currEdge : currSg.edgeIterable())
        sg.addEdge(currEdge.getGovernor(), currEdge.getDependent(),
                   currEdge.getRelation(), currEdge.getWeight(), currEdge.isExtra());
    }
    sg.setRoots(newRoots);
    return sg;
  }

  /**
   * Like makeFromGraphs, but it makes a deep copy of the graphs and
   * renumbers the index words.
   * <br>
   * <code>lengths</code> must be a vector containing the number of
   * tokens in each sentence.  This is used to reindex the tokens.
   */
  public static SemanticGraph deepCopyFromGraphs(List<SemanticGraph> graphs,
                                                 List<Integer> lengths) {
    SemanticGraph newGraph = new SemanticGraph();
    Map<Integer, IndexedWord> newWords = Generics.newHashMap();
    List<IndexedWord> newRoots = new ArrayList<IndexedWord>();
    int vertexOffset = 0;
    for (int i = 0; i < graphs.size(); ++i) {
      SemanticGraph graph = graphs.get(i);
      for (IndexedWord vertex : graph.vertexSet()) {
        IndexedWord newVertex = new IndexedWord(vertex);
        newVertex.setIndex(vertex.index() + vertexOffset);
        newGraph.addVertex(newVertex);
        newWords.put(newVertex.index(), newVertex);
      }
      for (SemanticGraphEdge edge : graph.edgeIterable()) {
        IndexedWord gov = newWords.get(edge.getGovernor().index() +
                                       vertexOffset);
        IndexedWord dep = newWords.get(edge.getDependent().index() +
                                       vertexOffset);
        if (gov == null || dep == null) {
          throw new AssertionError("Counting problem (or broken edge)");
        }
        newGraph.addEdge(gov, dep, edge.getRelation(), edge.getWeight(), edge.isExtra());
      }
      for (IndexedWord root : graph.getRoots()) {
        newRoots.add(newWords.get(root.index() + vertexOffset));
      }
      vertexOffset += lengths.get(i);
    }
    newGraph.setRoots(newRoots);
    return newGraph;
  }

}
