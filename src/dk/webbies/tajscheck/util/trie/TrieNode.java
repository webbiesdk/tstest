package dk.webbies.tajscheck.util.trie;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by erik1 on 16-01-2017.
 */
class TrieNode {
    // Copy-pasted from: http://www.programcreek.com/2014/05/leetcode-implement-trie-prefix-tree-java/
    char c;
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isLeaf;

    public TrieNode() {}

    public TrieNode(char c){
        this.c = c;
    }
}