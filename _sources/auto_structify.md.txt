AutoStructify Component
=======================
AutoStructify is a component in recommonmark that takes a parsed docutil AST by `CommonMarkParser`,
and transform it to another AST that introduces some of more. This enables additional features
of recommonmark syntax, to introduce more structure into the final generated document.


Configuring AutoStructify
-------------------------
The behavior of AutoStructify can be configured via a dict in document setting.
In sphinx, you can configure it by `conf.py`. The following snippet
is what is actually used to generate this document, see full code at [conf.py](conf.py).

```python
github_doc_root = 'https://github.com/rtfd/recommonmark/tree/master/doc/'
def setup(app):
    app.add_config_value('recommonmark_config', {
            'url_resolver': lambda url: github_doc_root + url,
            'auto_toc_tree_section': 'Contents',
            }, True)
    app.add_transform(AutoStructify)

```
All the features are by default enabled

***List of options***
* __enable_auto_toc_tree__: whether enable [Auto Toc Tree](#auto-toc-tree) feature.
* __auto_toc_tree_section__: when enabled,  [Auto Toc Tree](#auto-toc-tree) will only be enabled on section that matches the title.
* __enable_auto_doc_ref__: whether enable [Auto Doc Ref](#auto-doc-ref) feature.  **Deprecated**
* __enable_math__: whether enable [Math Formula](#math-formula)
* __enable_inline_math__: whether enable [Inline Math](#inline-math)
* __enable_eval_rst__: whether [Embed reStructuredText](#embed-restructuredtext) is enabled.
* __url_resolver__: a function that maps a existing relative position in the document to a http link

Auto Toc Tree
-------------
One of important command in tools like sphinx is `toctree`. This is a command to generate table of contents and
tell sphinx about the structure of the documents. In markdown, usually we manually list of contents by a bullet list
of url reference to the other documents.

AutoStructify will transforms bullet list of document URLs

```
* [Title1](doc1.md)
* [Title2](doc2.md)
```
will be translated to the AST of following reStructuredText code
```rst
.. toctree::
   :maxdepth: 1

   doc1
   doc2
```
You can also find the usage of this feature in `index.md` of this document.

Auto Doc Ref
------------

```eval_rst
.. note:: **This option is deprecated.**
    This option has been superseded by the default linking behavior, which
    will first try to resolve as an internal reference, and then as an
    external reference.
```

It is common to refer to another document page in one document. We usually use reference to do that.
AutoStructify will translate these reference block into a structured document reference. For example
```
[API Reference](api_ref.md)
```
will be translated to the AST of following reStructuredText code
```
:doc:`API Reference </api_ref>`
```
And it will be rendered as [API Reference](api_ref)

URL Resolver
------------
Sometimes in a markdown, we want to refer to the code in the same repo.
This can usually be done by a reference by reference path. However, since the generated document is hosted elsewhere,
the relative path may not work in generated document site. URL resolver is introduced to solve this problem.

Basically, you can define a function that maps an relative path of document to the http path that you wish to link to.
For example, the setting  mentioned in the beginning of this document used a resolver that maps the files to github.
So `[parser code](../recommonmark/parser.py)` will be translated into [parser code](../recommonmark/parser.py)

Note that the reference to the internal document will not be passed to url resolver, and will be linked to the internal document pages correctly, see [Auto Doc Ref](#auto-doc-ref).


Codeblock Extensions
--------------------
In markdown, you can write codeblocks fenced by (at least) three backticks
(```` ``` ````).  The following is an example of codeblock.

````
``` language
some code block
```
````

Codeblock extensions are mechanism that specialize certain codeblocks to different render behaviors.
The extension will be trigger by the language argument to the codeblck

### Syntax Highlight
You can highlight syntax of codeblocks by specifying the language you need. For example,

````
```python
def function():
    return True
```
````

will be rendered as

```python
def function():
    return True
```

### Math Formula
You can normally write latex math formula with `math` codeblock. See also [Inline Math](#inline-math).

Example

````
```math
E = m c^2
```
````

will be rendered as

```math
E = m c^2
```

### Embed reStructuredText
Recommonmark also allows embedding reStructuredText syntax in the codeblocks.
There are two styles for embedding reStructuredText. The first is enabled by `eval_rst` codeblock. The content of codeblock will be parsed as reStructuredText and insert into the document. This can be used to quickly introduce some of reStructuredText command that not yet available in markdown. For example,

````
```eval_rst
.. autoclass:: recommonmark.transform.AutoStructify
    :show-inheritance:
```
````

will be rendered as

```eval_rst
.. autoclass:: recommonmark.transform.AutoStructify
    :show-inheritance:
```

This example used to use sphinx autodoc to insert document of AutoStructify class definition into the document.

The second style is a shorthand of the above style. It allows you to leave off the eval_rst .. portion and directly render directives. For example,

````rst
``` important:: Its a note! in markdown!
```
````

will be rendered as

``` important:: Its a note! in markdown!
```

#### An Advanced Example

````rst
``` sidebar:: Line numbers and highlights

     emphasis-lines:
       highlights the lines.
     linenos:
       shows the line numbers as well.
     caption:
       shown at the top of the code block.
     name:
       may be referenced with `:ref:` later.
```

``` code-block:: markdown
     :linenos:
     :emphasize-lines: 3,5
     :caption: An example code-block with everything turned on.
     :name: Full code-block example

     # Comment line
     import System
     System.run_emphasis_line
     # Long lines in code blocks create a auto horizontal scrollbar
     System.exit!
```
````

will be rendered as

``` sidebar:: Line numbers and highlights

    emphasis-lines:
      highlights the lines.
    linenos:
      shows the line numbers as well.
    caption:
      shown at the top of the code block.
    name:
      may be referenced with `:ref:` later.
```

``` code-block:: markdown
    :linenos:
    :emphasize-lines: 3,5
    :caption: An example code-block with everything turned on.
    :name: Full code-block example

    # Comment line
    import System
    System.run_emphasis_line
    # Long lines in code blocks create a auto horizontal scrollbar
    System.exit!
```

The `<div style="clear: right;"></div>` line clears the sidebar for the next title.

<div style="clear: right;"></div>


Inline Math
-----------
Besides the [Math Formula](#math-formula), you can also write latex math in inline codeblock text. You can do so by inserting `$`
in the beginning and end of inline codeblock.

Example

```
This formula `$ y=\sum_{i=1}^n g(x_i) $`
```

will be rendered as:

This formula `$ y=\sum_{i=1}^n g(x_i) $`
