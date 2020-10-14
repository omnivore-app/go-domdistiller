// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.distiller;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.http.client.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DomUtil {
    /**
     * GWT does not provide a way to get a list of all attributes that have been explicitly set on a
     * DOM element (only a way to query the value of a particular attribute). In javascript, this
     * list is accessible as elem.attributes.
     *
     * @Return The element's attribute list from javascript.
     */
    public static native JsArray<Node> getAttributes(Element elem) /*-{
        return elem.attributes;
    }-*/;

    // Returns the first element with |className| in the tree rooted at |root|, null if none is
    // found.
    public static native Element getFirstElementWithClassName(Element root, String className) /*-{
        return root.querySelector("." + className);
    }-*/;

    // Returns the first element with |tagName| in the tree rooted at |root|.
    // null if none is found.
    public static Element getFirstElementByTagName(Element e, String tagName) {
        NodeList<Element> elements = e.getElementsByTagName(tagName);
        if (elements.getLength() > 0) {
            return elements.getItem(0);
        }
        return null;
    }

    public static native boolean hasClassName(Element elem, String className) /*-{
        return elem.classList.contains(className);
    }-*/;

    public static native JsArrayString getClassList(Element elem) /*-{
        return elem.classList;
    }-*/;

    /**
     * Check to see if a provided URL has the specified root domain (ex. http://a.b.c/foo/bar has
     * root domain of b.c).
     * @param url The URL to test.
     * @param root The root domain to test against.
     * @return True if url has the specified root domain.
     */
    public static boolean hasRootDomain(String url, String root) {
        if (url == null || root == null) {
            return false;
        }
        AnchorElement anchor = Document.get().createAnchorElement();
        anchor.setHref(url);
        String host = anchor.getPropertyString("host");
        return ("." + host).endsWith("." + root);
    }

    /**
     * Split URL parameters into key/value pairs and return them in a map.
     * @param query The query string after the "?".
     * @return Map of all query parameters or an empty map.
     */
    public static Map<String, String> splitUrlParams(String query) {
        if (query == null || query.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> paramMap = new HashMap<>();
        String[] params = query.split("&");
        for (int i = 0; i < params.length; i++) {}
        for (String currentParam : params) {
            String[] paramSplit = currentParam.split("=");
            if (paramSplit.length > 1) {
                paramMap.put(paramSplit[0], URL.decode(paramSplit[1]));
            }
        }
        return paramMap;
    }

    /**
      * @Return The CSS style of an element after applying the active stylesheets and resolving any
      * basic computation the style's value(s) may contain.
      * @param el - DOM element
    */
    public static native Style getComputedStyle(Element el) /*-{
      return getComputedStyle(el, null);
    }-*/;

    public static boolean isVisible(Element e) {
        Style style = getComputedStyle(e);
        double opacity = JavaScript.parseFloat(style.getOpacity());
        return !(style.getDisplay().equals("none") ||
                style.getVisibility().equals("hidden") ||
                opacity == 0.0F);
    }

    /**
     * Verifies if a given element is visible by checking its offset.
     */
    public static boolean isVisibleByOffset(Element e) {
        // Detect whether any of the ancestors has "display: none".
        // Using offsetParent alone wouldn't work because it's also null when position is fixed.
        // Using offsetHeight/Width alone makes sense in production, but we have too many
        // zero-sized elements in our tests.
        return e.getOffsetParent() != null || e.getOffsetHeight() != 0 || e.getOffsetWidth() != 0;
    }

    /**
     * Get the element of the main article, if any.
     * @return An element of article (not necessarily the html5 article element).
     */
    public static Element getArticleElement(Element root) {
        NodeList<Element> allArticles = root.getElementsByTagName("ARTICLE");
        List<Element> visibleElements = getVisibleElements(allArticles);
        // Having multiple article elements usually indicates a bad case for this shortcut.
        // TODO(wychen): some sites exclude things like title and author in article element.
        if (visibleElements.size() == 1) {
            return visibleElements.get(0);
        }
        // Note that the CSS property matching is case sensitive, and "Article" is the correct
        // capitalization.
        String query = "[itemscope][itemtype*=\"Article\"],[itemscope][itemtype*=\"Posting\"]";
        allArticles = DomUtil.querySelectorAll(root, query);
        visibleElements = getVisibleElements(allArticles);
        // It is commonly seen that the article is wrapped separately or in multiple layers.
        if (visibleElements.size() > 0) {
            return Element.as(DomUtil.getNearestCommonAncestor(visibleElements));
        }
        return null;
    }

    /**
     * Get a list of visible elements.
     * @return A list of visible elements.
     */
    public static List<Element> getVisibleElements(NodeList<Element> nodeList) {
        List<Element> visibleElements = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = nodeList.getItem(i);
            if (DomUtil.isVisible(element) &&
                    DomUtil.isVisibleByOffset(element) && DomUtil.getArea(element) > 0) {
                visibleElements.add(element);
            }
        }
        return visibleElements;
    }

    public static native double getTime() /*-{
        // window.performance is unavailable in Gwt's dev environment and even referencing it on iOS
        // causes a crash.
        if ((typeof distiller_on_ios === 'undefined' || !distiller_on_ios) && window.performance) {
          return window.performance.now();
        }
        return Date.now();
    }-*/;

    /**
     * Use jsni for direct access to javascript's textContent.  textContent is different from
     * innerText (see http://www.kellegous.com/j/2013/02/27/innertext-vs-textcontent):
     * - textContent is the raw textual content, doesn't require layout, and is basically a
     *   concatenation of the values of all text nodes within a subtree.
     * - innerText is what is presented to the user, requires layout, and excludes text in invisible
     *   elements, e.g. <title> tags.
     */
    public static native String javascriptTextContent(Node node) /*-{
        return node.textContent;
    }-*/;

    /**
     * Get all text from a tree/sub-tree. The node is added to the DOM for rendering, so that the
     * innerText has all the line breaks even if the node is not originally rendered.
     * See https://crbug.com/859410.
     * Note that this should only be used in tests since it modifies the DOM.
     * TODO(wychen): assert this is not used in prod if we have a way to do so.
     * @param node The root of the tree.
     * @return The text contained in this tree.
     */
    public static String getTextFromTreeForTest(Node node) {
        // Temporarily add the node to the DOM so that style is calculated.
        Document.get().getBody().appendChild(node);
        String output = DomUtil.getInnerText(node);

        // And remove it again.
        Document.get().getBody().removeChild(node);
        return output;
    }

    // Returns whether querySelectorAll is available
    public static native boolean supportQuerySelectorAll(Element root) /*-{
        return (typeof(root.querySelectorAll) == 'function');
    }-*/;

    // GWT doesn't support querySelectorAll, so testing the caller could be harder.
    public static native NodeList<Element> querySelectorAll(Node l, String selectors) /*-{
        return l.querySelectorAll(selectors);
    }-*/;

    public static native Document createHTMLDocument(Document doc) /*-{
        return doc.implementation.createHTMLDocument();
    }-*/;

    public static native Element getFirstElementChild(Document document) /*-{
        return document.firstElementChild;
    }-*/;
}
