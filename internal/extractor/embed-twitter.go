// ORIGINAL: java/extractors/embeds/TwitterExtractor.java

package extractor

import (
	nurl "net/url"
	"strings"

	"github.com/go-shiori/dom"
	"github.com/markusmobius/go-domdistiller/internal/domutil"
	"github.com/markusmobius/go-domdistiller/internal/webdoc"
	"golang.org/x/net/html"
)

// TwitterExtractor is used to look for Twitter embeds. This class will looks for
// both rendered and unrendered tweets.
type TwitterExtractor struct{}

func NewTwitterExtractor() *TwitterExtractor {
	return &TwitterExtractor{}
}

func (te *TwitterExtractor) RelevantTagNames() []string {
	tagNames := []string{}
	for tagName := range relevantTwitterTags {
		tagNames = append(tagNames, tagName)
	}
	return tagNames
}

func (te *TwitterExtractor) Extract(node *html.Node) webdoc.Element {
	if node == nil {
		return nil
	}

	nodeTagName := dom.TagName(node)
	if _, exist := relevantTwitterTags[nodeTagName]; !exist {
		return nil
	}

	if nodeTagName == "blockquote" {
		return te.extractNonRendered(node)
	} else {
		return te.extractRendered(node)
	}
}

// extractNonRendered handle a Twitter embed that has not yet been rendered.
func (te *TwitterExtractor) extractNonRendered(node *html.Node) webdoc.Element {
	// Make sure the characteristic class name for Twitter exists.
	if !strings.Contains(dom.GetAttribute(node, "class"), "twitter-tweet") {
		return nil
	}

	// Get the last anchor in this section; it should contain the tweet id.
	anchors := dom.GetElementsByTagName(node, "a")
	if len(anchors) == 0 {
		return nil
	}

	tweetAnchor := anchors[len(anchors)-1]
	tweetAnchorHref := dom.GetAttribute(tweetAnchor, "href")
	if !domutil.HasRootDomain(tweetAnchorHref, "twitter.com") {
		return nil
	}

	tweetID := te.getTweetIdFromURL(tweetAnchorHref)
	if tweetID == "" {
		return nil
	}

	return &webdoc.Embed{
		Element: node,
		Type:    "twitter",
		ID:      tweetID,
	}
}

// extractRendered handle a Twitter embed that has been rendered.
func (te *TwitterExtractor) extractRendered(node *html.Node) webdoc.Element {
	// Rendered tweet must be iframe
	if dom.TagName(node) != "iframe" {
		return nil
	}

	// Iframe must be for twitter.com
	iframeSrc := dom.GetAttribute(node, "src")
	if !domutil.HasRootDomain(iframeSrc, "twitter.com") {
		return nil
	}

	// In original dom-distiller they look for tweet id in blockquotes inside iframe.
	// However nowadays tweet ID is embedded as iframe's attribute.
	tweetID := dom.GetAttribute(node, "data-tweet-id")
	if tweetID == "" {
		return nil
	}

	return &webdoc.Embed{
		Element: node,
		Type:    "twitter",
		ID:      tweetID,
	}
}

func (te *TwitterExtractor) getTweetIdFromURL(tweetURL string) string {
	if strings.HasPrefix(tweetURL, "//") {
		tweetURL = "http:" + tweetURL
	}

	parsedURL, err := nurl.ParseRequestURI(tweetURL)
	if err != nil {
		return ""
	}

	// Tweet ID will be the last part of the path, account
	// for possible tail slash/empty path sections.
	pathParts := strings.Split(parsedURL.Path, "/")
	for i := len(pathParts) - 1; i >= 0; i-- {
		part := strings.TrimSpace(pathParts[i])
		if part != "" {
			return part
		}
	}

	return ""
}
