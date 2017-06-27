package dk.webbies.tajscheck.util.trie;

import java.util.Collection;
import java.util.Map;

/**
 * Created by erik1 on 16-01-2017.
 */
public class Trie {
    // Copy-pasted from: http://www.programcreek.com/2014/05/leetcode-implement-trie-prefix-tree-java/
    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public static Trie create(Collection<String> strings) {
        Trie result = new Trie();
        for (String string : strings) {
            result.insert(string);
        }
        return result;
    }

    // Inserts a word into the trie.
    public void insert(String word) {
        Map<Character, TrieNode> children = root.children;

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);

            TrieNode t;
            if (children.containsKey(c)) {
                t = children.get(c);
            } else {
                t = new TrieNode(c);
                children.put(c, t);
            }

            children = t.children;

            //set leaf node
            if (i == word.length() - 1)
                t.isLeaf = true;
        }
    }

    // Returns if the word is in the trie.
    public boolean search(String word) {
        TrieNode t = searchNode(word);

        return t != null && t.isLeaf;
    }

    public boolean containsChildren(String word) {
        TrieNode node = searchNode(word);

        return node != null && node.children.size() > 0;
    }

    // Returns if there is any word in the trie
    // that starts with the given prefix.
    public boolean startsWith(String prefix) {
        return searchNode(prefix) != null;
    }

    public TrieNode searchNode(String str) {
        Map<Character, TrieNode> children = root.children;
        TrieNode t = null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (children.containsKey(c)) {
                t = children.get(c);
                children = t.children;
            } else {
                return null;
            }
        }

        return t;
    }
}
