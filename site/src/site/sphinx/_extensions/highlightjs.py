import html

from docutils import nodes
from pygments.formatters.html import HtmlFormatter
from six import text_type
from sphinx.pygments_styles import NoneStyle
from sphinx.writers.html import HTMLTranslator


# Applies syntax highlighting to a literal block if it has a class 'highlight-<language>'.
def parsed_literal_visit_literal_block(self, node, next_visitor):
    classes = node.get('classes', [])
    lang = ''
    for c in classes:
        if c.startswith('highlight-'):
            lang = c[10:].strip()
            break

    if len(lang) == 0:
        return next_visitor(self, node)

    def warner(msg):
        self.builder.warn(msg, (self.builder.current_docname, node.line))

    self.body.append(self.highlighter.highlight_block(node.astext(), lang, warn=warner))

    raise nodes.SkipNode


class HljsHighlighter(object):

    def __init__(self, dest='html', stylename='', trim_doctest_flags=False):
        self.dest = dest
        self.trim_doctest_flags = trim_doctest_flags
        self.formatter_args = {'style': NoneStyle}
        self.formatter = HtmlFormatter

    def get_formatter(self, **kwargs):
        return self.formatter

    def unhighlighted(self, source):
        return '<pre>' + html.escape(source) + '</pre>\n'

    def highlight_block(self, source, lang, opts=None, location=None, force=False, **kwargs):
        if not isinstance(source, text_type):
            source = source.decode()

        if lang != None and lang != 'default':
            return '<div class="highlight hljs"><pre class="' + lang + '">' + html.escape(source) + '</pre></div>\n'
        else:
            return '<pre class="literal-block">' + html.escape(source) + '</pre>\n'

    def get_stylesheet(self):
        return ''


def override_highlighter(app):
    if app.builder.name == 'html':
        app.builder.highlighter = HljsHighlighter()


def setup(app):
    app.connect('builder-inited', override_highlighter)

    # Intercept the rendering of HTML literals.
    old_visitor = HTMLTranslator.visit_literal_block
    HTMLTranslator.visit_literal_block = lambda self, node: parsed_literal_visit_literal_block(self, node, old_visitor)

    pass
