/*
 * Copyright 2018, Robert Dyer, Mohd Arafat
 *                 and Bowling Green State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.graphs.ddg;

import boa.graphs.cfg.CFG;
import boa.graphs.cfg.CFGNode;
import boa.runtime.BoaAbstractFixP;
import boa.runtime.BoaAbstractTraversal;
import boa.types.Graph.Traversal;
import boa.types.Ast.*;

import java.util.*;

/**
 * Data Dependence Graph builder
 *
 * @author marafat
 */

public class DDG {

    private DDGNode entryNode;
    private Set<DDGNode> nodes = new HashSet<DDGNode>();
    private Map<DDGNode, Set<DDGNode>> defUseChain = new HashMap<DDGNode, Set<DDGNode>>();
    private Map<DDGNode, Set<DDGNode>> useDefChain; //TODO: needs reaching-def analysis

    public DDG(final CFG cfg) throws Exception {
        Map<Integer, InOut> liveVars = getLiveVariables(cfg);
        formDefUseChains(liveVars, cfg);
        constructDDG(liveVars.keySet());
    }

    public DDG(final Method m) throws Exception {
        this(new CFG(m));
    }

    // Getters
    public DDGNode getEntryNode() { return  entryNode; }

    public Set<DDGNode> getNodes() { return nodes; }

    public Map<DDGNode, Set<DDGNode>> getDefUseChain() {
        return defUseChain;
    }

    public Set<DDGNode> getUseNodes(final DDGNode node) {
        return defUseChain.get(node);
    }

    /**
     * Gives back all the def nodes for the given variable
     *
     * @param var variable
     * @return definition nodes
     */
    public Set<DDGNode> getDefNodes(final String var) {
        Set<DDGNode> defNodes = new HashSet<DDGNode>();
        for (DDGNode n: defUseChain.keySet()) {
            if (n.getDefVariable().equals(var))
                defNodes.add(n);
        }

        return defNodes;
    }

    /**
     * Gives back the node for the given node id, otherwise returns null
     *
     * @param id node id
     * @return DDGNode
     */
    public DDGNode getNode(int id) {
        for (DDGNode n: nodes)
            if (n.getId() == id)
                return n;

        return null;
    }

    /**
     * Computes in and out variables for each node
     *
     * @param cfg control flow graph
     * @return map of nodes and in, out variables
     * @throws Exception
     */
    private HashMap<Integer, InOut> getLiveVariables(final CFG cfg) throws Exception {
        BoaAbstractTraversal liveVar = new BoaAbstractTraversal<InOut>(true, true) {
            
            protected InOut preTraverse(final CFGNode node) throws Exception {
                InOut currentNode = new InOut();

                if ((getValue(node) != null)) {
                    currentNode = getValue(node);
                }

                // out = Union in[node.successor]
                for (CFGNode s: node.getSuccessorsList()) {
                    InOut succ = getValue(s);
                    if (succ != null)
                        currentNode.out.addAll(succ.in);
                }

                // out - def
                Set<Pair> currentDiff = new HashSet<Pair>(currentNode.out);
                if (!node.getDefVariables().equals(""))
                    for (Pair p: currentNode.out)
                        if (p.var.equals(node.getDefVariables()))
                            currentDiff.remove(p);

                // out = use Union (out - def)
                for (String var: node.getUseVariables())
                    currentNode.in.add(new Pair(var, node));
                currentNode.in.addAll(currentDiff);

                return currentNode;
            }

            @Override
            public void traverse(final CFGNode node, boolean flag) throws Exception {
                if(flag) {
                    currentResult = new InOut(preTraverse(node));
                    outputMapObj.put(node.getId(), new InOut(currentResult));
                }
                else
                    outputMapObj.put(node.getId(), new InOut(preTraverse(node)));
            }
        };

        BoaAbstractFixP fixp = new boa.runtime.BoaAbstractFixP() {

            public boolean invoke1(final InOut current, final InOut previous) throws Exception {
                InOut curr = new InOut(current);
                curr.in.removeAll(previous.in);
                return curr.in.size() == 0;
            }

            @Override
            public boolean invoke(Object current, Object previous) throws Exception{
                return invoke1((InOut) current, (InOut) previous);
            }
        };

        liveVar.traverse(cfg , Traversal.TraversalDirection.BACKWARD, Traversal.TraversalKind.POSTORDER, fixp);
        liveVar.outputMapObj.remove(cfg.getNodes().size()-1);

        return liveVar.outputMapObj;
    }

    /**
     * Constructs def-use chains for data flow between variables
     *
     * @param liveVar map of nodes and their in and out variables
     */
    private void formDefUseChains(final Map<Integer, InOut> liveVar, CFG cfg) {
        //match def variable of the node with the out variable. If the match occurs form a def-use mapping
        for (Integer nid: liveVar.keySet()) {
            CFGNode n = cfg.getNode(nid);
            DDGNode defNode = getNode(n);
            if (nid != 0) {
                for (Pair p : liveVar.get(nid).out) {
                    if (!n.getDefVariables().equals("")) {
                        if (n.getDefVariables().equals(p.var)) {
                            DDGNode useNode = getNode(p.node);
                            if (!defUseChain.containsKey(defNode))
                                defUseChain.put(defNode, new HashSet<DDGNode>());
                            defUseChain.get(defNode).add(useNode);
                            // connect nodes for constructing the graph
                            defNode.addSuccessor(useNode);
                            useNode.addPredecessor(defNode);
                            DDGEdge edge = new DDGEdge(defNode, useNode, p.var);
                            defNode.addOutEdge(edge);
                            useNode.addinEdge(edge);
                        }
                    }
                }
            }
        }
    }

    /**
     * Connects the disconnected nodes to form the DDG graph
     *
     * @param nodeids set of all node ids of the graph
     */
    private void constructDDG(Set<Integer> nodeids) {
        entryNode = getNode(0);
        for (int i: nodeids) {
            if (i != 0) {
                DDGNode dest = getNode(i);
                if (dest.getPredecessors().size() == 0 ||
                        (dest.getPredecessors().size() == 1 && dest.getPredecessors().get(0).equals(dest))) {
                    entryNode.addSuccessor(dest);
                    dest.addPredecessor(entryNode);
                    DDGEdge edge = new DDGEdge(entryNode, dest);
                    entryNode.addOutEdge(edge);
                    dest.addinEdge(edge);
                }
            }
        }
    }

    /**
     * Checks if a node already exists and returns it, otherwise returns a new node.
     *
     * @param cfgNode a post dominator tree node
     * @return a new DDG node or an existing DDG node
     */
    private DDGNode getNode(final CFGNode cfgNode) {
        DDGNode node = new DDGNode(cfgNode.getId());
        if (nodes.contains(node)) {
            for (DDGNode n : nodes) {
                if (n.equals(node))
                    return n;
            }
        }
        node.setStmt(cfgNode.getStmt());
        node.setExpr(cfgNode.getExpr());
        node.setDefVariable(cfgNode.getDefVariables());
        node.setUseVariables(cfgNode.getUseVariables());
        node.setKind(cfgNode.getKind());
        nodes.add(node);

        return node;
    }

    // Holds in and out pairs for each node
    private class InOut {
        Set<Pair> in;
        Set<Pair> out;

        InOut() {
            in = new HashSet<Pair>();
            out = new HashSet<Pair>();
        }

        InOut(final HashSet<Pair> in, final HashSet<Pair> out){
            this.in = in;
            this.out = out;
        }

        InOut(final InOut inout){
            this.in = new HashSet<Pair>(inout.in);
            this.out = new HashSet<Pair>(inout.out);
        }

        public InOut clone() {
            return new InOut(this);
        }
    }

    // (Var, Usenode) pair: use nodes are needed to construct def-use chains
    private class Pair {
        String var;
        CFGNode node;

        Pair() {}

        Pair(final String var, final CFGNode node) {
            this.var = var;
            this.node = node;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair pair = (Pair) o;

            if (!var.equals(pair.var)) return false;
            return node.getId() == pair.node.getId();
        }

        @Override
        public int hashCode() {
            int result = var.hashCode();
            result = 31 * result + node.hashCode();
            return result;
        }
    }

}