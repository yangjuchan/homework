/**********************************************************************

  numeric.c -

  $Author: akr $
  created at: Fri Aug 13 18:33:09 JST 1993

  Copyright (C) 1993-2007 Yukihiro Matsumoto

**********************************************************************/

#include "ruby/ruby.h"
#include "ruby/encoding.h"
#include <ctype.h>
#include <math.h>
#include <stdio.h>

#include "objc.h"

#if defined(__FreeBSD__) && __FreeBSD__ < 4
#include <floatingpoint.h>
#endif

#ifdef HAVE_FLOAT_H
#include <float.h>
#endif

#ifdef HAVE_IEEEFP_H
#include <ieeefp.h>
#endif

/* use IEEE 64bit values if not defined */
#ifndef FLT_RADIX
#define FLT_RADIX 2
#endif
#ifndef FLT_ROUNDS
#define FLT_ROUNDS 1
#endif
#ifndef DBL_MIN
#define DBL_MIN 2.2250738585072014e-308
#endif
#ifndef DBL_MAX
#define DBL_MAX 1.7976931348623157e+308
#endif
#ifndef DBL_MIN_EXP
#define DBL_MIN_EXP (-1021)
#endif
#ifndef DBL_MAX_EXP
#define DBL_MAX_EXP 1024
#endif
#ifndef DBL_MIN_10_EXP
#define DBL_MIN_10_EXP (-307)
#endif
#ifndef DBL_MAX_10_EXP
#define DBL_MAX_10_EXP 308
#endif
#ifndef DBL_DIG
#define DBL_DIG 15
#endif
#ifndef DBL_MANT_DIG
#define DBL_MANT_DIG 53
#endif
#ifndef DBL_EPSILON
#define DBL_EPSILON 2.2204460492503131e-16
#endif

#ifndef HAVE_ROUND
double
round(double x)
{
    double f;

    if (x > 0.0) {
	f = floor(x);
	x = f + (x - f >= 0.5);
    }
    else if (x < 0.0) {
	f = ceil(x);
	x = f - (f - x >= 0.5);
    }
    return x;
}
#endif

static ID id_coerce, id_to_i, id_eq;
#if WITH_OBJC
static ID id_spaceship, id_pow, id_shl, id_shr;
#endif

VALUE rb_cNumeric;
#if WITH_OBJC
VALUE rb_cCFNumber;
VALUE rb_cNSNumber;
#endif
VALUE rb_cFloat;
VALUE rb_cInteger;
VALUE rb_cFixnum;

VALUE rb_eZeroDivError;
VALUE rb_eFloatDomainError;

#if WITH_OBJC
static CFMutableDictionaryRef fixnum_dict = NULL;
static struct RFixnum *fixnum_cache = NULL;

VALUE
rb_box_fixnum(VALUE fixnum)
{
    struct RFixnum *val;
    long value;

    if (fixnum_dict == NULL)
	fixnum_dict = CFDictionaryCreateMutable(kCFAllocatorMalloc, 0, NULL, NULL); 

    value = FIX2LONG(fixnum);

    if (value >= 0 && value <= 1000000) {
	if (fixnum_cache == NULL) {
	   fixnum_cache = (struct RFixnum *)calloc(1, sizeof(struct RFixnum) * 1000000);
	}
	val = &fixnum_cache[value];
	if (val->klass == 0) {
	    val->klass = rb_cFixnum;
	    val->value = value;
	}
	return (VALUE)val;
    }

    val = (struct RFixnum *)CFDictionaryGetValue(fixnum_dict, (const void *)fixnum);
    if (val == NULL) {
	val = (struct RFixnum *)malloc(sizeof(struct RFixnum));
	val->klass = rb_cFixnum;
	val->value = FIX2LONG(fixnum);
	CFDictionarySetValue(fixnum_dict, (const void *)fixnum, (const void *)val);
    }

    return (VALUE)val;
}
#endif

void
rb_num_zerodiv(void)
{
    rb_raise(rb_eZeroDivError, "divided by 0");
}


/*
 *  call-seq:
 *     num.coerce(numeric)   => array
 *
 *  If <i>aNumeric</i> is the same type as <i>num</i>, returns an array
 *  containing <i>aNumeric</i> and <i>num</i>. Otherwise, returns an
 *  array with both <i>aNumeric</i> and <i>num</i> represented as
 *  <code>Float</code> objects. This coercion mechanism is used by
 *  Ruby to handle mixed-type numeric operations: it is intended to
 *  find a compatible common type between the two operands of the operator.
 *
 *     1.coerce(2.5)   #=> [2.5, 1.0]
 *     1.2.coerce(3)   #=> [3.0, 1.2]
 *     1.coerce(2)     #=> [2, 1]
 */

static VALUE
num_coerce(VALUE x, VALUE y)
{
    if (CLASS_OF(x) == CLASS_OF(y))
	return rb_assoc_new(y, x);
    return rb_assoc_new(rb_Float(y), rb_Float(x));
}

static VALUE
coerce_body(VALUE *x)
{
    return rb_funcall(x[1], id_coerce, 1, x[0]);
}

static VALUE
coerce_rescue(VALUE *x)
{
    volatile VALUE v = rb_inspect(x[1]);

    rb_raise(rb_eTypeError, "%s can't be coerced into %s",
	     rb_special_const_p(x[1])?
	     RSTRING_BYTEPTR(v):
	     rb_obj_classname(x[1]),
	     rb_obj_classname(x[0]));
    return Qnil;		/* dummy */
}

static int
do_coerce(VALUE *x, VALUE *y, int err)
{
    VALUE ary;
    VALUE a[2];

    a[0] = *x; a[1] = *y;

    ary = rb_rescue(coerce_body, (VALUE)a, err?coerce_rescue:0, (VALUE)a);
    if (TYPE(ary) != T_ARRAY || RARRAY_LEN(ary) != 2) {
	if (err) {
	    rb_raise(rb_eTypeError, "coerce must return [x, y]");
	}
	return Qfalse;
    }

    *x = RARRAY_AT(ary, 0);
    *y = RARRAY_AT(ary, 1);
    return Qtrue;
}

VALUE
rb_num_coerce_bin(VALUE x, VALUE y, ID func)
{
    do_coerce(&x, &y, Qtrue);
    return rb_funcall(x, func, 1, y);
}

VALUE
rb_num_coerce_cmp(VALUE x, VALUE y, ID func)
{
    if (do_coerce(&x, &y, Qfalse))
	return rb_funcall(x, func, 1, y);
    return Qnil;
}

VALUE
rb_num_coerce_relop(VALUE x, VALUE y, ID func)
{
    VALUE c, x0 = x, y0 = y;

    if (!do_coerce(&x, &y, Qfalse) ||
	NIL_P(c = rb_funcall(x, func, 1, y))) {
	rb_cmperr(x0, y0);
	return Qnil;		/* not reached */
    }
    return c;
}

/*
 * Trap attempts to add methods to <code>Numeric</code> objects. Always
 * raises a <code>TypeError</code>
 */

static VALUE
num_sadded(VALUE x, VALUE name)
{
    /* ruby_frame = ruby_frame->prev; */ /* pop frame for "singleton_method_added" */
    /* Numerics should be values; singleton_methods should not be added to them */
    rb_raise(rb_eTypeError,
	     "can't define singleton method \"%s\" for %s",
	     rb_id2name(rb_to_id(name)),
	     rb_obj_classname(x));
    return Qnil;		/* not reached */
}

/* :nodoc: */
static VALUE
num_init_copy(VALUE x, VALUE y)
{
    /* Numerics are immutable values, which should not be copied */
    rb_raise(rb_eTypeError, "can't copy %s", rb_obj_classname(x));
    return Qnil;		/* not reached */
}

/*
 *  call-seq:
 *     +num    => num
 *
 *  Unary Plus---Returns the receiver's value.
 */

static VALUE
num_uplus(VALUE num)
{
    return num;
}

/*
 *  call-seq:
 *     -num    => numeric
 *
 *  Unary Minus---Returns the receiver's value, negated.
 */

static VALUE
num_uminus(VALUE num)
{
    VALUE zero;

    zero = INT2FIX(0);
    do_coerce(&zero, &num, Qtrue);

    return rb_funcall(zero, '-', 1, num);
}

/*
 *  call-seq:
 *     num.quo(numeric)    =>   result
 *
 *  Returns most exact division (rational for integers, float for floats).
 */

static VALUE
num_quo(VALUE x, VALUE y)
{
    return rb_funcall(rb_rational_raw1(x), '/', 1, y);
}


/*
 *  call-seq:
 *     num.fdiv(numeric)    =>   float
 *
 *  Returns float division.
 */

static VALUE
num_fdiv(VALUE x, VALUE y)
{
    return rb_funcall(rb_Float(x), '/', 1, y);
}


static VALUE num_floor(VALUE num);

/*
 *  call-seq:
 *     num.div(numeric)    => integer
 *
 *  Uses <code>/</code> to perform division, then converts the result to
 *  an integer. <code>Numeric</code> does not define the <code>/</code>
 *  operator; this is left to subclasses.
 */

static VALUE
num_div(VALUE x, VALUE y)
{
    if (rb_equal(INT2FIX(0), y)) rb_num_zerodiv();
    return num_floor(rb_funcall(x, '/', 1, y));
}


/*
 *  call-seq:
 *     num.divmod( aNumeric ) -> anArray
 *
 *  Returns an array containing the quotient and modulus obtained by
 *  dividing <i>num</i> by <i>aNumeric</i>. If <code>q, r =
 *  x.divmod(y)</code>, then
 *
 *      q = floor(float(x)/float(y))
 *      x = q*y + r
 *
 *  The quotient is rounded toward -infinity, as shown in the following table:
 *
 *     a    |  b  |  a.divmod(b)  |   a/b   | a.modulo(b) | a.remainder(b)
 *    ------+-----+---------------+---------+-------------+---------------
 *     13   |  4  |   3,    1     |   3     |    1        |     1
 *    ------+-----+---------------+---------+-------------+---------------
 *     13   | -4  |  -4,   -3     |  -3     |   -3        |     1
 *    ------+-----+---------------+---------+-------------+---------------
 *    -13   |  4  |  -4,    3     |  -4     |    3        |    -1
 *    ------+-----+---------------+---------+-------------+---------------
 *    -13   | -4  |   3,   -1     |   3     |   -1        |    -1
 *    ------+-----+---------------+---------+-------------+---------------
 *     11.5 |  4  |   2,    3.5   |   2.875 |    3.5      |     3.5
 *    ------+-----+---------------+---------+-------------+---------------
 *     11.5 | -4  |  -3,   -0.5   |  -2.875 |   -0.5      |     3.5
 *    ------+-----+---------------+---------+-------------+---------------
 *    -11.5 |  4  |  -3,    0.5   |  -2.875 |    0.5      |    -3.5
 *    ------+-----+---------------+---------+-------------+---------------
 *    -11.5 | -4  |   2,   -3.5   |   2.875 |   -3.5      |    -3.5
 *
 *
 *  Examples
 *
 *     11.divmod(3)         #=> [3, 2]
 *     11.divmod(-3)        #=> [-4, -1]
 *     11.divmod(3.5)       #=> [3, 0.5]
 *     (-11).divmod(3.5)    #=> [-4, 3.0]
 *     (11.5).divmod(3.5)   #=> [3, 1.0]
 */

static VALUE
num_divmod(VALUE x, VALUE y)
{
    return rb_assoc_new(num_div(x, y), rb_funcall(x, '%', 1, y));
}

/*
 *  call-seq:
 *     num.modulo(numeric)    => result
 *
 *  Equivalent to
 *  <i>num</i>.<code>divmod(</code><i>aNumeric</i><code>)[1]</code>.
 */

static VALUE
num_modulo(VALUE x, VALUE y)
{
    return rb_funcall(x, '%', 1, y);
}

/*
 *  call-seq:
 *     num.remainder(numeric)    => result
 *
 *  If <i>num</i> and <i>numeric</i> have different signs, returns
 *  <em>mod</em>-<i>numeric</i>; otherwise, returns <em>mod</em>. In
 *  both cases <em>mod</em> is the value
 *  <i>num</i>.<code>modulo(</code><i>numeric</i><code>)</code>. The
 *  differences between <code>remainder</code> and modulo
 *  (<code>%</code>) are shown in the table under <code>Numeric#divmod</code>.
 */

static VALUE
num_remainder(VALUE x, VALUE y)
{
    VALUE z = rb_funcall(x, '%', 1, y);

    if ((!rb_equal(z, INT2FIX(0))) &&
	((RTEST(rb_funcall(x, '<', 1, INT2FIX(0))) &&
	  RTEST(rb_funcall(y, '>', 1, INT2FIX(0)))) ||
	 (RTEST(rb_funcall(x, '>', 1, INT2FIX(0))) &&
	  RTEST(rb_funcall(y, '<', 1, INT2FIX(0)))))) {
	return rb_funcall(z, '-', 1, y);
    }
    return z;
}

/*
 *  call-seq:
 *     num.scalar? -> true or false
 *
 *  Returns <code>true</code> if <i>num</i> is an <code>Scalar</code>
 *  (i.e. non <code>Complex</code>).
 */

static VALUE
num_scalar_p(VALUE num)
{
    return Qtrue;
}

/*
 *  call-seq:
 *     num.integer? -> true or false
 *
 *  Returns <code>true</code> if <i>num</i> is an <code>Integer</code>
 *  (including <code>Fixnum</code> and <code>Bignum</code>).
 */

static VALUE
num_int_p(VALUE num)
{
    return Qfalse;
}

/*
 *  call-seq:
 *     num.abs   => num or numeric
 *
 *  Returns the absolute value of <i>num</i>.
 *
 *     12.abs         #=> 12
 *     (-34.56).abs   #=> 34.56
 *     -34.56.abs     #=> 34.56
 */

static VALUE
num_abs(VALUE num)
{
    if (RTEST(rb_funcall(num, '<', 1, INT2FIX(0)))) {
	return rb_funcall(num, rb_intern("-@"), 0);
    }
    return num;
}


/*
 *  call-seq:
 *     num.zero?    => true or false
 *
 *  Returns <code>true</code> if <i>num</i> has a zero value.
 */

static VALUE
num_zero_p(VALUE num)
{
    if (rb_equal(num, INT2FIX(0))) {
	return Qtrue;
    }
    return Qfalse;
}


/*
 *  call-seq:
 *     num.nonzero?    => num or nil
 *
 *  Returns <i>num</i> if <i>num</i> is not zero, <code>nil</code>
 *  otherwise. This behavior is useful when chaining comparisons:
 *
 *     a = %w( z Bb bB bb BB a aA Aa AA A )
 *     b = a.sort {|a,b| (a.downcase <=> b.downcase).nonzero? || a <=> b }
 *     b   #=> ["A", "a", "AA", "Aa", "aA", "BB", "Bb", "bB", "bb", "z"]
 */

static VALUE
num_nonzero_p(VALUE num)
{
    if (RTEST(rb_funcall(num, rb_intern("zero?"), 0, 0))) {
	return Qnil;
    }
    return num;
}

/*
 *  call-seq:
 *     num.to_int    => integer
 *
 *  Invokes the child class's <code>to_i</code> method to convert
 *  <i>num</i> to an integer.
 */

static VALUE
num_to_int(VALUE num)
{
    return rb_funcall(num, id_to_i, 0, 0);
}


/********************************************************************
 *
 * Document-class: Float
 *
 *  <code>Float</code> objects represent real numbers using the native
 *  architecture's double-precision floating point representation.
 */

VALUE
rb_float_new(double d)
{
    NEWOBJ(flt, struct RFloat);
    OBJSETUP(flt, rb_cFloat, T_FLOAT);

    flt->float_value = d;
    return (VALUE)flt;
}

/*
 *  call-seq:
 *     flt.to_s    => string
 *
 *  Returns a string containing a representation of self. As well as a
 *  fixed or exponential form of the number, the call may return
 *  ``<code>NaN</code>'', ``<code>Infinity</code>'', and
 *  ``<code>-Infinity</code>''.
 */

static VALUE
flo_to_s(VALUE flt)
{
    char buf[32];
    double value = RFLOAT_VALUE(flt);
    char *p, *e;

    if (isinf(value))
	return rb_usascii_str_new2(value < 0 ? "-Infinity" : "Infinity");
    else if(isnan(value))
	return rb_usascii_str_new2("NaN");

    sprintf(buf, "%#.15g", value); /* ensure to print decimal point */
    if (!(e = strchr(buf, 'e'))) {
	e = buf + strlen(buf);
    }
    if (!ISDIGIT(e[-1])) { /* reformat if ended with decimal point (ex 111111111111111.) */
	sprintf(buf, "%#.14e", value);
	if (!(e = strchr(buf, 'e'))) {
	    e = buf + strlen(buf);
	}
    }
    p = e;
    while (p[-1]=='0' && ISDIGIT(p[-2]))
	p--;
    memmove(p, e, strlen(e)+1);
    return rb_usascii_str_new2(buf);
}

/*
 * MISSING: documentation
 */

static VALUE
flo_coerce(VALUE x, VALUE y)
{
    return rb_assoc_new(rb_Float(y), x);
}

/*
 * call-seq:
 *    -float   => float
 *
 * Returns float, negated.
 */

static VALUE
flo_uminus(VALUE flt)
{
    return DOUBLE2NUM(-RFLOAT_VALUE(flt));
}

/*
 * call-seq:
 *   float + other   => float
 *
 * Returns a new float which is the sum of <code>float</code>
 * and <code>other</code>.
 */

static VALUE
flo_plus(VALUE x, VALUE y)
{
    switch (TYPE(y)) {
      case T_FIXNUM:
	return DOUBLE2NUM(RFLOAT_VALUE(x) + (double)FIX2LONG(y));
      case T_BIGNUM:
	return DOUBLE2NUM(RFLOAT_VALUE(x) + rb_big2dbl(y));
      case T_FLOAT:
	return DOUBLE2NUM(RFLOAT_VALUE(x) + RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '+');
    }
}

/*
 * call-seq:
 *   float + other   => float
 *
 * Returns a new float which is the difference of <code>float</code>
 * and <code>other</code>.
 */

static VALUE
flo_minus(VALUE x, VALUE y)
{
    switch (TYPE(y)) {
      case T_FIXNUM:
	return DOUBLE2NUM(RFLOAT_VALUE(x) - (double)FIX2LONG(y));
      case T_BIGNUM:
	return DOUBLE2NUM(RFLOAT_VALUE(x) - rb_big2dbl(y));
      case T_FLOAT:
	return DOUBLE2NUM(RFLOAT_VALUE(x) - RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '-');
    }
}

/*
 * call-seq:
 *   float * other   => float
 *
 * Returns a new float which is the product of <code>float</code>
 * and <code>other</code>.
 */

static VALUE
flo_mul(VALUE x, VALUE y)
{
    switch (TYPE(y)) {
      case T_FIXNUM:
	return DOUBLE2NUM(RFLOAT_VALUE(x) * (double)FIX2LONG(y));
      case T_BIGNUM:
	return DOUBLE2NUM(RFLOAT_VALUE(x) * rb_big2dbl(y));
      case T_FLOAT:
	return DOUBLE2NUM(RFLOAT_VALUE(x) * RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '*');
    }
}

/*
 * call-seq:
 *   float / other   => float
 *
 * Returns a new float which is the result of dividing
 * <code>float</code> by <code>other</code>.
 */

static VALUE
flo_div(VALUE x, VALUE y)
{
    long f_y;
    double d;

    switch (TYPE(y)) {
      case T_FIXNUM:
	f_y = FIX2LONG(y);
	return DOUBLE2NUM(RFLOAT_VALUE(x) / (double)f_y);
      case T_BIGNUM:
	d = rb_big2dbl(y);
	return DOUBLE2NUM(RFLOAT_VALUE(x) / d);
      case T_FLOAT:
	return DOUBLE2NUM(RFLOAT_VALUE(x) / RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '/');
    }
}

static VALUE
flo_quo(VALUE x, VALUE y)
{
    return rb_funcall(x, '/', 1, y);
}

static void
flodivmod(double x, double y, double *divp, double *modp)
{
    double div, mod;

#ifdef HAVE_FMOD
    mod = fmod(x, y);
#else
    {
	double z;

	modf(x/y, &z);
	mod = x - z * y;
    }
#endif
    if (isinf(x) && !isinf(y) && !isnan(y))
	div = x;
    else
	div = (x - mod) / y;
    if (y*mod < 0) {
	mod += y;
	div -= 1.0;
    }
    if (modp) *modp = mod;
    if (divp) *divp = div;
}


/*
 *  call-seq:
 *     flt % other         => float
 *     flt.modulo(other)   => float
 *
 *  Return the modulo after division of <code>flt</code> by <code>other</code>.
 *
 *     6543.21.modulo(137)      #=> 104.21
 *     6543.21.modulo(137.24)   #=> 92.9299999999996
 */

static VALUE
flo_mod(VALUE x, VALUE y)
{
    double fy, mod;

    switch (TYPE(y)) {
      case T_FIXNUM:
	fy = (double)FIX2LONG(y);
	break;
      case T_BIGNUM:
	fy = rb_big2dbl(y);
	break;
      case T_FLOAT:
	fy = RFLOAT_VALUE(y);
	break;
      default:
	return rb_num_coerce_bin(x, y, '%');
    }
    flodivmod(RFLOAT_VALUE(x), fy, 0, &mod);
    return DOUBLE2NUM(mod);
}

static VALUE
dbl2ival(double d)
{
    if (FIXABLE(d)) {
	d = round(d);
	return LONG2FIX((long)d);
    }
    else if (isnan(d) || isinf(d)) {
	/* special case: cannot return integer value */
	return rb_float_new(d);
    }
    else {
	return rb_dbl2big(d);
    }
}

/*
 *  call-seq:
 *     flt.divmod(numeric)    => array
 *
 *  See <code>Numeric#divmod</code>.
 */

static VALUE
flo_divmod(VALUE x, VALUE y)
{
    double fy, div, mod;
    volatile VALUE a, b;

    switch (TYPE(y)) {
      case T_FIXNUM:
	fy = (double)FIX2LONG(y);
	break;
      case T_BIGNUM:
	fy = rb_big2dbl(y);
	break;
      case T_FLOAT:
	fy = RFLOAT_VALUE(y);
	break;
      default:
	return rb_num_coerce_bin(x, y, rb_intern("divmod"));
    }
    flodivmod(RFLOAT_VALUE(x), fy, &div, &mod);
    a = dbl2ival(div);
    b = DOUBLE2NUM(mod);
    return rb_assoc_new(a, b);
}

/*
 * call-seq:
 *
 *  flt ** other   => float
 *
 * Raises <code>float</code> the <code>other</code> power.
 */

static VALUE
flo_pow(VALUE x, VALUE y)
{
    switch (TYPE(y)) {
      case T_FIXNUM:
	return DOUBLE2NUM(pow(RFLOAT_VALUE(x), (double)FIX2LONG(y)));
      case T_BIGNUM:
	return DOUBLE2NUM(pow(RFLOAT_VALUE(x), rb_big2dbl(y)));
      case T_FLOAT:
	return DOUBLE2NUM(pow(RFLOAT_VALUE(x), RFLOAT_VALUE(y)));
      default:
	return rb_num_coerce_bin(x, y, rb_intern("**"));
    }
}

/*
 *  call-seq:
 *     num.eql?(numeric)    => true or false
 *
 *  Returns <code>true</code> if <i>num</i> and <i>numeric</i> are the
 *  same type and have equal values.
 *
 *     1 == 1.0          #=> true
 *     1.eql?(1.0)       #=> false
 *     (1.0).eql?(1.0)   #=> true
 */

static VALUE
num_eql(VALUE x, VALUE y)
{
    if (TYPE(x) != TYPE(y)) return Qfalse;

    return rb_equal(x, y);
}

/*
 *  call-seq:
 *     num <=> other -> 0 or nil
 *
 *  Returns zero if <i>num</i> equals <i>other</i>, <code>nil</code>
 *  otherwise.
 */

static VALUE
num_cmp(VALUE x, VALUE y)
{
    if (x == y) return INT2FIX(0);
    return Qnil;
}

static VALUE
num_equal(VALUE x, VALUE y)
{
    if (x == y) return Qtrue;
    return rb_funcall(y, id_eq, 1, x);
}

/*
 *  call-seq:
 *     flt == obj   => true or false
 *
 *  Returns <code>true</code> only if <i>obj</i> has the same value
 *  as <i>flt</i>. Contrast this with <code>Float#eql?</code>, which
 *  requires <i>obj</i> to be a <code>Float</code>.
 *
 *     1.0 == 1   #=> true
 *
 */

static VALUE
flo_eq(VALUE x, VALUE y)
{
    volatile double a, b;

    switch (TYPE(y)) {
      case T_FIXNUM:
	b = FIX2LONG(y);
	break;
      case T_BIGNUM:
	b = rb_big2dbl(y);
	break;
      case T_FLOAT:
	b = RFLOAT_VALUE(y);
	if (isnan(b)) return Qfalse;
	break;
      default:
	return num_equal(x, y);
    }
    a = RFLOAT_VALUE(x);
    if (isnan(a)) return Qfalse;
    return (a == b)?Qtrue:Qfalse;
}

/*
 * call-seq:
 *   flt.hash   => integer
 *
 * Returns a hash code for this float.
 */

static VALUE
flo_hash(VALUE num)
{
    double d;
    int hash;

    d = RFLOAT_VALUE(num);
    hash = rb_memhash(&d, sizeof(d));
    return INT2FIX(hash);
}

VALUE
rb_dbl_cmp(double a, double b)
{
    if (isnan(a) || isnan(b)) return Qnil;
    if (a == b) return INT2FIX(0);
    if (a > b) return INT2FIX(1);
    if (a < b) return INT2FIX(-1);
    return Qnil;
}

/*
 *  call-seq:
 *     flt <=> numeric   => -1, 0, +1
 *
 *  Returns -1, 0, or +1 depending on whether <i>flt</i> is less than,
 *  equal to, or greater than <i>numeric</i>. This is the basis for the
 *  tests in <code>Comparable</code>.
 */

static VALUE
flo_cmp(VALUE x, VALUE y)
{
    double a, b;

    a = RFLOAT_VALUE(x);
    switch (TYPE(y)) {
      case T_FIXNUM:
	b = (double)FIX2LONG(y);
	break;

      case T_BIGNUM:
	b = rb_big2dbl(y);
	break;

      case T_FLOAT:
	b = RFLOAT_VALUE(y);
	break;

      default:
	return rb_num_coerce_cmp(x, y, rb_intern("<=>"));
    }
    return rb_dbl_cmp(a, b);
}

/*
 * call-seq:
 *   flt > other    =>  true or false
 *
 * <code>true</code> if <code>flt</code> is greater than <code>other</code>.
 */

static VALUE
flo_gt(VALUE x, VALUE y)
{
    double a, b;

    a = RFLOAT_VALUE(x);
    switch (TYPE(y)) {
      case T_FIXNUM:
	b = (double)FIX2LONG(y);
	break;

      case T_BIGNUM:
	b = rb_big2dbl(y);
	break;

      case T_FLOAT:
	b = RFLOAT_VALUE(y);
	if (isnan(b)) return Qfalse;
	break;

      default:
	return rb_num_coerce_relop(x, y, '>');
    }
    if (isnan(a)) return Qfalse;
    return (a > b)?Qtrue:Qfalse;
}

/*
 * call-seq:
 *   flt >= other    =>  true or false
 *
 * <code>true</code> if <code>flt</code> is greater than
 * or equal to <code>other</code>.
 */

static VALUE
flo_ge(VALUE x, VALUE y)
{
    double a, b;

    a = RFLOAT_VALUE(x);
    switch (TYPE(y)) {
      case T_FIXNUM:
	b = (double)FIX2LONG(y);
	break;

      case T_BIGNUM:
	b = rb_big2dbl(y);
	break;

      case T_FLOAT:
	b = RFLOAT_VALUE(y);
	if (isnan(b)) return Qfalse;
	break;

      default:
	return rb_num_coerce_relop(x, y, rb_intern(">="));
    }
    if (isnan(a)) return Qfalse;
    return (a >= b)?Qtrue:Qfalse;
}

/*
 * call-seq:
 *   flt < other    =>  true or false
 *
 * <code>true</code> if <code>flt</code> is less than <code>other</code>.
 */

static VALUE
flo_lt(VALUE x, VALUE y)
{
    double a, b;

    a = RFLOAT_VALUE(x);
    switch (TYPE(y)) {
      case T_FIXNUM:
	b = (double)FIX2LONG(y);
	break;

      case T_BIGNUM:
	b = rb_big2dbl(y);
	break;

      case T_FLOAT:
	b = RFLOAT_VALUE(y);
	if (isnan(b)) return Qfalse;
	break;

      default:
	return rb_num_coerce_relop(x, y, '<');
    }
    if (isnan(a)) return Qfalse;
    return (a < b)?Qtrue:Qfalse;
}

/*
 * call-seq:
 *   flt <= other    =>  true or false
 *
 * <code>true</code> if <code>flt</code> is less than
 * or equal to <code>other</code>.
 */

static VALUE
flo_le(VALUE x, VALUE y)
{
    double a, b;

    a = RFLOAT_VALUE(x);
    switch (TYPE(y)) {
      case T_FIXNUM:
	b = (double)FIX2LONG(y);
	break;

      case T_BIGNUM:
	b = rb_big2dbl(y);
	break;

      case T_FLOAT:
	b = RFLOAT_VALUE(y);
	if (isnan(b)) return Qfalse;
	break;

      default:
	return rb_num_coerce_relop(x, y, rb_intern("<="));
    }
    if (isnan(a)) return Qfalse;
    return (a <= b)?Qtrue:Qfalse;
}

/*
 *  call-seq:
 *     flt.eql?(obj)   => true or false
 *
 *  Returns <code>true</code> only if <i>obj</i> is a
 *  <code>Float</code> with the same value as <i>flt</i>. Contrast this
 *  with <code>Float#==</code>, which performs type conversions.
 *
 *     1.0.eql?(1)   #=> false
 */

static VALUE
flo_eql(VALUE x, VALUE y)
{
    if (TYPE(y) == T_FLOAT) {
	double a = RFLOAT_VALUE(x);
	double b = RFLOAT_VALUE(y);

	if (isnan(a) || isnan(b)) return Qfalse;
	if (a == b) return Qtrue;
    }
    return Qfalse;
}

/*
 * call-seq:
 *   flt.to_f   => flt
 *
 * As <code>flt</code> is already a float, returns <i>self</i>.
 */

static VALUE
flo_to_f(VALUE num)
{
    return num;
}

/*
 *  call-seq:
 *     flt.abs    => float
 *
 *  Returns the absolute value of <i>flt</i>.
 *
 *     (-34.56).abs   #=> 34.56
 *     -34.56.abs     #=> 34.56
 *
 */

static VALUE
flo_abs(VALUE flt)
{
    double val = fabs(RFLOAT_VALUE(flt));
    return DOUBLE2NUM(val);
}

/*
 *  call-seq:
 *     flt.zero? -> true or false
 *
 *  Returns <code>true</code> if <i>flt</i> is 0.0.
 *
 */

static VALUE
flo_zero_p(VALUE num)
{
    if (RFLOAT_VALUE(num) == 0.0) {
	return Qtrue;
    }
    return Qfalse;
}

/*
 *  call-seq:
 *     flt.nan? -> true or false
 *
 *  Returns <code>true</code> if <i>flt</i> is an invalid IEEE floating
 *  point number.
 *
 *     a = -1.0      #=> -1.0
 *     a.nan?        #=> false
 *     a = 0.0/0.0   #=> NaN
 *     a.nan?        #=> true
 */

static VALUE
flo_is_nan_p(VALUE num)
{
    double value = RFLOAT_VALUE(num);

    return isnan(value) ? Qtrue : Qfalse;
}

/*
 *  call-seq:
 *     flt.infinite? -> nil, -1, +1
 *
 *  Returns <code>nil</code>, -1, or +1 depending on whether <i>flt</i>
 *  is finite, -infinity, or +infinity.
 *
 *     (0.0).infinite?        #=> nil
 *     (-1.0/0.0).infinite?   #=> -1
 *     (+1.0/0.0).infinite?   #=> 1
 */

static VALUE
flo_is_infinite_p(VALUE num)
{
    double value = RFLOAT_VALUE(num);

    if (isinf(value)) {
	return INT2FIX( value < 0 ? -1 : 1 );
    }

    return Qnil;
}

/*
 *  call-seq:
 *     flt.finite? -> true or false
 *
 *  Returns <code>true</code> if <i>flt</i> is a valid IEEE floating
 *  point number (it is not infinite, and <code>nan?</code> is
 *  <code>false</code>).
 *
 */

static VALUE
flo_is_finite_p(VALUE num)
{
    double value = RFLOAT_VALUE(num);

#if HAVE_FINITE
    if (!finite(value))
	return Qfalse;
#else
    if (isinf(value) || isnan(value))
	return Qfalse;
#endif

    return Qtrue;
}

/*
 *  call-seq:
 *     flt.floor   => integer
 *
 *  Returns the largest integer less than or equal to <i>flt</i>.
 *
 *     1.2.floor      #=> 1
 *     2.0.floor      #=> 2
 *     (-1.2).floor   #=> -2
 *     (-2.0).floor   #=> -2
 */

static VALUE
flo_floor(VALUE num)
{
    double f = floor(RFLOAT_VALUE(num));
    long val;

    if (!FIXABLE(f)) {
	return rb_dbl2big(f);
    }
    val = f;
    return LONG2FIX(val);
}

/*
 *  call-seq:
 *     flt.ceil    => integer
 *
 *  Returns the smallest <code>Integer</code> greater than or equal to
 *  <i>flt</i>.
 *
 *     1.2.ceil      #=> 2
 *     2.0.ceil      #=> 2
 *     (-1.2).ceil   #=> -1
 *     (-2.0).ceil   #=> -2
 */

static VALUE
flo_ceil(VALUE num)
{
    double f = ceil(RFLOAT_VALUE(num));
    long val;

    if (!FIXABLE(f)) {
	return rb_dbl2big(f);
    }
    val = f;
    return LONG2FIX(val);
}

/*
 *  call-seq:
 *     flt.round([ndigits])   => integer or float
 *
 *  Rounds <i>flt</i> to a given precision in decimal digits (default 0 digits).
 *  Precision may be negative.  Returns a a floating point number when ndigits
 *  is more than one.
 *
 *     1.5.round      #=> 2
 *     (-1.5).round   #=> -2
 */

static VALUE
flo_round(int argc, VALUE *argv, VALUE num)
{
    VALUE nd;
    double number, f;
    int ndigits = 0, i;
    long val;

    if (argc > 0 && rb_scan_args(argc, argv, "01", &nd) == 1) {
	ndigits = NUM2INT(nd);
    }
    number  = RFLOAT_VALUE(num);
    f = 1.0;
    i = abs(ndigits);
    while  (--i >= 0)
	f = f*10.0;

    if (ndigits < 0) number /= f;
    else number *= f;
    number = round(number);
    if (ndigits < 0) number *= f;
    else number /= f;

    if (ndigits > 0) return DOUBLE2NUM(number);

    if (!FIXABLE(number)) {
	return rb_dbl2big(number);
    }
    val = number;
    return LONG2FIX(val);
}

/*
 *  call-seq:
 *     flt.to_i       => integer
 *     flt.to_int     => integer
 *     flt.truncate   => integer
 *
 *  Returns <i>flt</i> truncated to an <code>Integer</code>.
 */

static VALUE
flo_truncate(VALUE num)
{
    double f = RFLOAT_VALUE(num);
    long val;

    if (f > 0.0) f = floor(f);
    if (f < 0.0) f = ceil(f);

    if (!FIXABLE(f)) {
	return rb_dbl2big(f);
    }
    val = f;
    return LONG2FIX(val);
}


/*
 *  call-seq:
 *     num.floor    => integer
 *
 *  Returns the largest integer less than or equal to <i>num</i>.
 *  <code>Numeric</code> implements this by converting <i>anInteger</i>
 *  to a <code>Float</code> and invoking <code>Float#floor</code>.
 *
 *     1.floor      #=> 1
 *     (-1).floor   #=> -1
 */

static VALUE
num_floor(VALUE num)
{
    return flo_floor(rb_Float(num));
}


/*
 *  call-seq:
 *     num.ceil    => integer
 *
 *  Returns the smallest <code>Integer</code> greater than or equal to
 *  <i>num</i>. Class <code>Numeric</code> achieves this by converting
 *  itself to a <code>Float</code> then invoking
 *  <code>Float#ceil</code>.
 *
 *     1.ceil        #=> 1
 *     1.2.ceil      #=> 2
 *     (-1.2).ceil   #=> -1
 *     (-1.0).ceil   #=> -1
 */

static VALUE
num_ceil(VALUE num)
{
    return flo_ceil(rb_Float(num));
}

/*
 *  call-seq:
 *     num.round([ndigits])    => integer or float
 *
 *  Rounds <i>num</i> to a given precision in decimal digits (default 0 digits).
 *  Precision may be negative.  Returns a a floating point number when ndigits
 *  is more than one.  <code>Numeric</code> implements this by converting itself
 *  to a <code>Float</code> and invoking <code>Float#round</code>.
 */

static VALUE
num_round(int argc, VALUE* argv, VALUE num)
{
    return flo_round(argc, argv, rb_Float(num));
}

/*
 *  call-seq:
 *     num.truncate    => integer
 *
 *  Returns <i>num</i> truncated to an integer. <code>Numeric</code>
 *  implements this by converting its value to a float and invoking
 *  <code>Float#truncate</code>.
 */

static VALUE
num_truncate(VALUE num)
{
    return flo_truncate(rb_Float(num));
}


/*
 *  call-seq:
 *     num.step(limit, step ) {|i| block }     => num
 *
 *  Invokes <em>block</em> with the sequence of numbers starting at
 *  <i>num</i>, incremented by <i>step</i> on each call. The loop
 *  finishes when the value to be passed to the block is greater than
 *  <i>limit</i> (if <i>step</i> is positive) or less than
 *  <i>limit</i> (if <i>step</i> is negative). If all the arguments are
 *  integers, the loop operates using an integer counter. If any of the
 *  arguments are floating point numbers, all are converted to floats,
 *  and the loop is executed <i>floor(n + n*epsilon)+ 1</i> times,
 *  where <i>n = (limit - num)/step</i>. Otherwise, the loop
 *  starts at <i>num</i>, uses either the <code><</code> or
 *  <code>></code> operator to compare the counter against
 *  <i>limit</i>, and increments itself using the <code>+</code>
 *  operator.
 *
 *     1.step(10, 2) { |i| print i, " " }
 *     Math::E.step(Math::PI, 0.2) { |f| print f, " " }
 *
 *  <em>produces:</em>
 *
 *     1 3 5 7 9
 *     2.71828182845905 2.91828182845905 3.11828182845905
 */

static VALUE
num_step(int argc, VALUE *argv, VALUE from)
{
    VALUE to, step;

    RETURN_ENUMERATOR(from, argc, argv);
    if (argc == 1) {
	to = argv[0];
	step = INT2FIX(1);
    }
    else {
	if (argc == 2) {
	    to = argv[0];
	    step = argv[1];
	}
	else {
	    rb_raise(rb_eArgError, "wrong number of arguments");
	}
	if (rb_equal(step, INT2FIX(0))) {
	    rb_raise(rb_eArgError, "step can't be 0");
	}
    }

    if (FIXNUM_P(from) && FIXNUM_P(to) && FIXNUM_P(step)) {
	long i, end, diff;

	i = FIX2LONG(from);
	end = FIX2LONG(to);
	diff = FIX2LONG(step);

	if (diff > 0) {
	    while (i <= end) {
		rb_yield(LONG2FIX(i));
		i += diff;
	    }
	}
	else {
	    while (i >= end) {
		rb_yield(LONG2FIX(i));
		i += diff;
	    }
	}
    }
    else if (TYPE(from) == T_FLOAT || TYPE(to) == T_FLOAT || TYPE(step) == T_FLOAT) {
	const double epsilon = DBL_EPSILON;
	double beg = NUM2DBL(from);
	double end = NUM2DBL(to);
	double unit = NUM2DBL(step);
	double n = (end - beg)/unit;
	double err = (fabs(beg) + fabs(end) + fabs(end-beg)) / fabs(unit) * epsilon;
	long i;

	if (err>0.5) err=0.5;
	n = floor(n + err) + 1;
	for (i=0; i<n; i++) {
	    rb_yield(DOUBLE2NUM(i*unit+beg));
	}
    }
    else {
	VALUE i = from;
	ID cmp;

	if (RTEST(rb_funcall(step, '>', 1, INT2FIX(0)))) {
	    cmp = '>';
	}
	else {
	    cmp = '<';
	}
	for (;;) {
	    if (RTEST(rb_funcall(i, cmp, 1, to))) break;
	    rb_yield(i);
	    i = rb_funcall(i, '+', 1, step);
	}
    }
    return from;
}

SIGNED_VALUE
rb_num2long(VALUE val)
{
  again:
    if (NIL_P(val)) {
	rb_raise(rb_eTypeError, "no implicit conversion from nil to integer");
    }

    if (FIXNUM_P(val)) return FIX2LONG(val);

    switch (TYPE(val)) {
      case T_FLOAT:
	if (RFLOAT_VALUE(val) <= (double)LONG_MAX
	    && RFLOAT_VALUE(val) >= (double)LONG_MIN) {
	    return (SIGNED_VALUE)(RFLOAT_VALUE(val));
	}
	else {
	    char buf[24];
	    char *s;

	    sprintf(buf, "%-.10g", RFLOAT_VALUE(val));
	    if ((s = strchr(buf, ' ')) != 0) *s = '\0';
	    rb_raise(rb_eRangeError, "float %s out of range of integer", buf);
	}

      case T_BIGNUM:
	return rb_big2long(val);

      default:
	val = rb_to_int(val);
	goto again;
    }
}

VALUE
rb_num2ulong(VALUE val)
{
    if (TYPE(val) == T_BIGNUM) {
	return rb_big2ulong(val);
    }
    return (VALUE)rb_num2long(val);
}

#if SIZEOF_INT < SIZEOF_VALUE
static void
check_int(SIGNED_VALUE num)
{
    const char *s;

    if (num < INT_MIN) {
	s = "small";
    }
    else if (num > INT_MAX) {
	s = "big";
    }
    else {
	return;
    }
    rb_raise(rb_eRangeError, "integer %"PRIdVALUE " too %s to convert to `int'", num, s);
}

static void
check_uint(VALUE num)
{
    if (num > UINT_MAX) {
	rb_raise(rb_eRangeError, "integer %"PRIuVALUE " too big to convert to `unsigned int'", num);
    }
}

long
rb_num2int(VALUE val)
{
    long num = rb_num2long(val);

    check_int(num);
    return num;
}

long
rb_fix2int(VALUE val)
{
    long num = FIXNUM_P(val)?FIX2LONG(val):rb_num2long(val);

    check_int(num);
    return num;
}

unsigned long
rb_num2uint(VALUE val)
{
    unsigned long num = rb_num2ulong(val);

    if (RTEST(rb_funcall(INT2FIX(0), '<', 1, val))) {
	check_uint(num);
    }
    return num;
}

unsigned long
rb_fix2uint(VALUE val)
{
    unsigned long num;

    if (!FIXNUM_P(val)) {
	return rb_num2uint(val);
    }
    num = FIX2ULONG(val);
    if (FIX2LONG(val) > 0) {
	check_uint(num);
    }
    return num;
}
#else
long
rb_num2int(VALUE val)
{
    return rb_num2long(val);
}

long
rb_fix2int(VALUE val)
{
    return FIX2INT(val);
}
#endif

VALUE
rb_num2fix(VALUE val)
{
    long v;

    if (FIXNUM_P(val)) return val;

    v = rb_num2long(val);
    if (!FIXABLE(v))
	rb_raise(rb_eRangeError, "integer %"PRIdVALUE " out of range of fixnum", v);
    return LONG2FIX(v);
}

#if HAVE_LONG_LONG

LONG_LONG
rb_num2ll(VALUE val)
{
    if (NIL_P(val)) {
	rb_raise(rb_eTypeError, "no implicit conversion from nil");
    }

    if (FIXNUM_P(val)) return (LONG_LONG)FIX2LONG(val);

    switch (TYPE(val)) {
      case T_FLOAT:
	if (RFLOAT_VALUE(val) <= (double)LLONG_MAX
	    && RFLOAT_VALUE(val) >= (double)LLONG_MIN) {
	    return (LONG_LONG)(RFLOAT_VALUE(val));
	}
	else {
	    char buf[24];
	    char *s;

	    sprintf(buf, "%-.10g", RFLOAT_VALUE(val));
	    if ((s = strchr(buf, ' ')) != 0) *s = '\0';
	    rb_raise(rb_eRangeError, "float %s out of range of long long", buf);
	}

      case T_BIGNUM:
	return rb_big2ll(val);

      case T_STRING:
	rb_raise(rb_eTypeError, "no implicit conversion from string");
	return Qnil;            /* not reached */

      case T_TRUE:
      case T_FALSE:
	rb_raise(rb_eTypeError, "no implicit conversion from boolean");
	return Qnil;		/* not reached */

      default:
	val = rb_to_int(val);
	return NUM2LL(val);
    }
}

unsigned LONG_LONG
rb_num2ull(VALUE val)
{
    if (TYPE(val) == T_BIGNUM) {
	return rb_big2ull(val);
    }
    return (unsigned LONG_LONG)rb_num2ll(val);
}

#endif  /* HAVE_LONG_LONG */

static VALUE
num_numerator(VALUE num)
{
    return rb_funcall(rb_Rational1(num), rb_intern("numerator"), 0);
}

static VALUE
num_denominator(VALUE num)
{
    return rb_funcall(rb_Rational1(num), rb_intern("denominator"), 0);
}

/*
 * Document-class: Integer
 *
 *  <code>Integer</code> is the basis for the two concrete classes that
 *  hold whole numbers, <code>Bignum</code> and <code>Fixnum</code>.
 *
 */


/*
 *  call-seq:
 *     int.to_i      => int
 *     int.to_int    => int
 *     int.floor     => int
 *     int.ceil      => int
 *     int.round     => int
 *     int.truncate  => int
 *
 *  As <i>int</i> is already an <code>Integer</code>, all these
 *  methods simply return the receiver.
 */

static VALUE
int_to_i(VALUE num)
{
    return num;
}

/*
 *  call-seq:
 *     int.integer? -> true
 *
 *  Always returns <code>true</code>.
 */

static VALUE
int_int_p(VALUE num)
{
    return Qtrue;
}

/*
 *  call-seq:
 *     int.odd? -> true or false
 *
 *  Returns <code>true</code> if <i>int</i> is an odd number.
 */

static VALUE
int_odd_p(VALUE num)
{
    if (rb_funcall(num, '%', 1, INT2FIX(2)) != INT2FIX(0)) {
	return Qtrue;
    }
    return Qfalse;
}

/*
 *  call-seq:
 *     int.even? -> true or false
 *
 *  Returns <code>true</code> if <i>int</i> is an even number.
 */

static VALUE
int_even_p(VALUE num)
{
    if (rb_funcall(num, '%', 1, INT2FIX(2)) == INT2FIX(0)) {
	return Qtrue;
    }
    return Qfalse;
}

/*
 *  call-seq:
 *     fixnum.next    => integer
 *     fixnum.succ    => integer
 *
 *  Returns the <code>Integer</code> equal to <i>int</i> + 1.
 *
 *     1.next      #=> 2
 *     (-1).next   #=> 0
 */

static VALUE
fix_succ(VALUE num)
{
    long i = FIX2LONG(num) + 1;
    return LONG2NUM(i);
}

/*
 *  call-seq:
 *     int.next    => integer
 *     int.succ    => integer
 *
 *  Returns the <code>Integer</code> equal to <i>int</i> + 1.
 *
 *     1.next      #=> 2
 *     (-1).next   #=> 0
 */

static VALUE
int_succ(VALUE num)
{
    if (FIXNUM_P(num)) {
	long i = FIX2LONG(num) + 1;
	return LONG2NUM(i);
    }
    return rb_funcall(num, '+', 1, INT2FIX(1));
}

/*
 *  call-seq:
 *     int.pred    => integer
 *
 *  Returns the <code>Integer</code> equal to <i>int</i> - 1.
 *
 *     1.pred      #=> 0
 *     (-1).pred   #=> -2
 */

static VALUE
int_pred(VALUE num)
{
    if (FIXNUM_P(num)) {
	long i = FIX2LONG(num) - 1;
	return LONG2NUM(i);
    }
    return rb_funcall(num, '-', 1, INT2FIX(1));
}

/*
 *  call-seq:
 *     int.chr([encoding])    => string
 *
 *  Returns a string containing the character represented by the
 *  receiver's value according to +encoding+.
 *
 *     65.chr    #=> "A"
 *     230.chr   #=> "\346"
 *     255.chr(Encoding::UTF_8)   #=> "\303\277"
 */

static VALUE
int_chr(int argc, VALUE *argv, VALUE num)
{
    char c;
#if !WITH_OBJC
    int n;
#endif
    long i = NUM2LONG(num);
    rb_encoding *enc;
    VALUE str;

    switch (argc) {
      case 0:
	if (i < 0 || 0xff < i) {
#if !WITH_OBJC
	  out_of_range:
#endif
	    rb_raise(rb_eRangeError, "%"PRIdVALUE " out of char range", i);
	}
	c = i;
	if (i < 0x80) {
	    return rb_usascii_str_new(&c, 1);
	}
	else {
	    return rb_str_new(&c, 1);
	}
      case 1:
	break;
      default:
	rb_raise(rb_eArgError, "wrong number of arguments (%d for 0 or 1)", argc);
	break;
    }
#if WITH_OBJC
    enc = rb_to_encoding(argv[0]);
    str = rb_enc_str_new(&c, 1, enc);
#else
    enc = rb_to_encoding(argv[0]);
    if (!enc) enc = rb_ascii8bit_encoding();
    if (i < 0 || (n = rb_enc_codelen(i, enc)) <= 0) goto out_of_range;
    str = rb_enc_str_new(0, n, enc);
    rb_enc_mbcput(i, RSTRING_BYTEPTR(str), enc);
#endif
    return str;
}

static VALUE
int_numerator(VALUE num)
{
    return num;
}

static VALUE
int_denominator(VALUE num)
{
    return INT2FIX(1);
}

/********************************************************************
 *
 * Document-class: Fixnum
 *
 *  A <code>Fixnum</code> holds <code>Integer</code> values that can be
 *  represented in a native machine word (minus 1 bit). If any operation
 *  on a <code>Fixnum</code> exceeds this range, the value is
 *  automatically converted to a <code>Bignum</code>.
 *
 *  <code>Fixnum</code> objects have immediate value. This means that
 *  when they are assigned or passed as parameters, the actual object is
 *  passed, rather than a reference to that object. Assignment does not
 *  alias <code>Fixnum</code> objects. There is effectively only one
 *  <code>Fixnum</code> object instance for any given integer value, so,
 *  for example, you cannot add a singleton method to a
 *  <code>Fixnum</code>.
 */


/*
 * call-seq:
 *   Fixnum.induced_from(obj)    =>  fixnum
 *
 * Convert <code>obj</code> to a Fixnum. Works with numeric parameters.
 * Also works with Symbols, but this is deprecated.
 */

static VALUE
rb_fix_induced_from(VALUE klass, VALUE x)
{
    return rb_num2fix(x);
}

/*
 * call-seq:
 *   Integer.induced_from(obj)    =>  fixnum, bignum
 *
 * Convert <code>obj</code> to an Integer.
 */

static VALUE
rb_int_induced_from(VALUE klass, VALUE x)
{
    switch (TYPE(x)) {
      case T_FIXNUM:
      case T_BIGNUM:
	return x;
      case T_FLOAT:
      case T_RATIONAL:
	return rb_funcall(x, id_to_i, 0);
      default:
	rb_raise(rb_eTypeError, "failed to convert %s into Integer",
		 rb_obj_classname(x));
    }
}

/*
 * call-seq:
 *   Float.induced_from(obj)    =>  float
 *
 * Convert <code>obj</code> to a float.
 */

static VALUE
rb_flo_induced_from(VALUE klass, VALUE x)
{
    switch (TYPE(x)) {
      case T_FIXNUM:
      case T_BIGNUM:
      case T_RATIONAL:
	return rb_funcall(x, rb_intern("to_f"), 0);
      case T_FLOAT:
	return x;
      default:
	rb_raise(rb_eTypeError, "failed to convert %s into Float",
		 rb_obj_classname(x));
    }
}

/*
 * call-seq:
 *   -fix   =>  integer
 *
 * Negates <code>fix</code> (which might return a Bignum).
 */

static VALUE
fix_uminus(VALUE num)
{
    return LONG2NUM(-FIX2LONG(num));
}

VALUE
rb_fix2str(VALUE x, int base)
{
    extern const char ruby_digitmap[];
    char buf[SIZEOF_VALUE*CHAR_BIT + 2], *b = buf + sizeof buf;
    long val = FIX2LONG(x);
    int neg = 0;

    if (base < 2 || 36 < base) {
	rb_raise(rb_eArgError, "invalid radix %d", base);
    }
    if (val == 0) {
	return rb_usascii_str_new2("0");
    }
    if (val < 0) {
	val = -val;
	neg = 1;
    }
    *--b = '\0';
    do {
	*--b = ruby_digitmap[(int)(val % base)];
    } while (val /= base);
    if (neg) {
	*--b = '-';
    }

    return rb_usascii_str_new2(b);
}

/*
 *  call-seq:
 *     fix.to_s( base=10 ) -> aString
 *
 *  Returns a string containing the representation of <i>fix</i> radix
 *  <i>base</i> (between 2 and 36).
 *
 *     12345.to_s       #=> "12345"
 *     12345.to_s(2)    #=> "11000000111001"
 *     12345.to_s(8)    #=> "30071"
 *     12345.to_s(10)   #=> "12345"
 *     12345.to_s(16)   #=> "3039"
 *     12345.to_s(36)   #=> "9ix"
 *
 */
static VALUE
fix_to_s(int argc, VALUE *argv, VALUE x)
{
    int base;

    if (argc == 0) base = 10;
    else {
	VALUE b;

	rb_scan_args(argc, argv, "01", &b);
	base = NUM2INT(b);
    }

    return rb_fix2str(x, base);
}

/*
 * call-seq:
 *   fix + numeric   =>  numeric_result
 *
 * Performs addition: the class of the resulting object depends on
 * the class of <code>numeric</code> and on the magnitude of the
 * result.
 */

static VALUE
fix_plus(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	long a, b, c;
	VALUE r;

	a = FIX2LONG(x);
	b = FIX2LONG(y);
	c = a + b;
	r = LONG2NUM(c);

	return r;
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return rb_big_plus(y, x);
      case T_FLOAT:
	return DOUBLE2NUM((double)FIX2LONG(x) + RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '+');
    }
}

/*
 * call-seq:
 *   fix - numeric   =>  numeric_result
 *
 * Performs subtraction: the class of the resulting object depends on
 * the class of <code>numeric</code> and on the magnitude of the
 * result.
 */

static VALUE
fix_minus(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	long a, b, c;
	VALUE r;

	a = FIX2LONG(x);
	b = FIX2LONG(y);
	c = a - b;
	r = LONG2NUM(c);

	return r;
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	x = rb_int2big(FIX2LONG(x));
	return rb_big_minus(x, y);
      case T_FLOAT:
	return DOUBLE2NUM((double)FIX2LONG(x) - RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '-');
    }
}

#define SQRT_LONG_MAX ((SIGNED_VALUE)1<<((SIZEOF_LONG*CHAR_BIT-1)/2))
/*tests if N*N would overflow*/
#define FIT_SQRT_LONG(n) (((n)<SQRT_LONG_MAX)&&((n)>=-SQRT_LONG_MAX))

/*
 * call-seq:
 *   fix * numeric   =>  numeric_result
 *
 * Performs multiplication: the class of the resulting object depends on
 * the class of <code>numeric</code> and on the magnitude of the
 * result.
 */

static VALUE
fix_mul(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
#ifdef __HP_cc
/* avoids an optimization bug of HP aC++/ANSI C B3910B A.06.05 [Jul 25 2005] */
	volatile
#endif
	SIGNED_VALUE a, b;
#if SIZEOF_VALUE * 2 <= SIZEOF_LONG_LONG
	LONG_LONG d;
#else
	SIGNED_VALUE c;
	VALUE r;
#endif

	a = FIX2LONG(x);
	b = FIX2LONG(y);

#if SIZEOF_VALUE * 2 <= SIZEOF_LONG_LONG
	d = (LONG_LONG)a * b;
	if (FIXABLE(d)) return LONG2FIX(d);
	return rb_ll2inum(d);
#else
	if (FIT_SQRT_LONG(a) && FIT_SQRT_LONG(b))
	    return LONG2FIX(a*b);
	c = a * b;
	r = LONG2FIX(c);

	if (a == 0) return x;
	if (FIX2LONG(r) != c || c/a != b) {
	    r = rb_big_mul(rb_int2big(a), rb_int2big(b));
	}
	return r;
#endif
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return rb_big_mul(y, x);
      case T_FLOAT:
	return DOUBLE2NUM((double)FIX2LONG(x) * RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, '*');
    }
}

static void
fixdivmod(long x, long y, long *divp, long *modp)
{
    long div, mod;

    if (y == 0) rb_num_zerodiv();
    if (y < 0) {
	if (x < 0)
	    div = -x / -y;
	else
	    div = - (x / -y);
    }
    else {
	if (x < 0)
	    div = - (-x / y);
	else
	    div = x / y;
    }
    mod = x - div*y;
    if ((mod < 0 && y > 0) || (mod > 0 && y < 0)) {
	mod += y;
	div -= 1;
    }
    if (divp) *divp = div;
    if (modp) *modp = mod;
}

/*
 *  call-seq:
 *     fix.fdiv(numeric)   => float
 *
 *  Returns the floating point result of dividing <i>fix</i> by
 *  <i>numeric</i>.
 *
 *     654321.fdiv(13731)      #=> 47.6528293642124
 *     654321.fdiv(13731.24)   #=> 47.6519964693647
 *
 */

static VALUE
fix_fdiv(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	return DOUBLE2NUM((double)FIX2LONG(x) / (double)FIX2LONG(y));
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return DOUBLE2NUM((double)FIX2LONG(x) / rb_big2dbl(y));
      case T_FLOAT:
	return DOUBLE2NUM((double)FIX2LONG(x) / RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_bin(x, y, rb_intern("fdiv"));
    }
}

static VALUE
fix_divide(VALUE x, VALUE y, ID op)
{
    if (FIXNUM_P(y)) {
	long div;

	fixdivmod(FIX2LONG(x), FIX2LONG(y), &div, 0);
	return LONG2NUM(div);
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	x = rb_int2big(FIX2LONG(x));
	return rb_big_div(x, y);
      case T_FLOAT:
	{
	    double div;

	    if (op == '/') {
		div = (double)FIX2LONG(x) / RFLOAT_VALUE(y);
		return DOUBLE2NUM(div);
	    }
	    else {
		if (RFLOAT_VALUE(y) == 0) rb_num_zerodiv();
		div = (double)FIX2LONG(x) / RFLOAT_VALUE(y);
		return rb_dbl2big(floor(div));
	    }
	}
      default:
	return rb_num_coerce_bin(x, y, op);
    }
}

/*
 * call-seq:
 *   fix / numeric      =>  numeric_result
 *
 * Performs division: the class of the resulting object depends on
 * the class of <code>numeric</code> and on the magnitude of the
 * result.
 */

static VALUE
fix_div(VALUE x, VALUE y)
{
    return fix_divide(x, y, '/');
}

/*
 * call-seq:
 *   fix.div(numeric)   =>  numeric_result
 *
 * Performs integer division: returns integer value.
 */

static VALUE
fix_idiv(VALUE x, VALUE y)
{
    return fix_divide(x, y, rb_intern("div"));
}

/*
 *  call-seq:
 *    fix % other         => Numeric
 *    fix.modulo(other)   => Numeric
 *
 *  Returns <code>fix</code> modulo <code>other</code>.
 *  See <code>Numeric.divmod</code> for more information.
 */

static VALUE
fix_mod(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	long mod;

	fixdivmod(FIX2LONG(x), FIX2LONG(y), 0, &mod);
	return LONG2NUM(mod);
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	x = rb_int2big(FIX2LONG(x));
	return rb_big_modulo(x, y);
      case T_FLOAT:
	{
	    double mod;

	    flodivmod((double)FIX2LONG(x), RFLOAT_VALUE(y), 0, &mod);
	    return DOUBLE2NUM(mod);
	}
      default:
	return rb_num_coerce_bin(x, y, '%');
    }
}

/*
 *  call-seq:
 *     fix.divmod(numeric)    => array
 *
 *  See <code>Numeric#divmod</code>.
 */
static VALUE
fix_divmod(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	long div, mod;

	fixdivmod(FIX2LONG(x), FIX2LONG(y), &div, &mod);

	return rb_assoc_new(LONG2NUM(div), LONG2NUM(mod));
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	x = rb_int2big(FIX2LONG(x));
	return rb_big_divmod(x, y);
      case T_FLOAT:
	{
	    double div, mod;
	    volatile VALUE a, b;

	    flodivmod((double)FIX2LONG(x), RFLOAT_VALUE(y), &div, &mod);
	    a = dbl2ival(div);
	    b = DOUBLE2NUM(mod);
	    return rb_assoc_new(a, b);
	}
      default:
	return rb_num_coerce_bin(x, y, rb_intern("divmod"));
    }
}

static VALUE
int_pow(long x, unsigned long y)
{
    int neg = x < 0;
    long z = 1;

    if (neg) x = -x;
    if (y & 1)
	z = x;
    else
	neg = 0;
    y &= ~1;
    do {
	while (y % 2 == 0) {
	    if (!FIT_SQRT_LONG(x)) {
		VALUE v;
	      bignum:
		v = rb_big_pow(rb_int2big(x), LONG2NUM(y));
		if (z != 1) v = rb_big_mul(rb_int2big(neg ? -z : z), v);
		return v;
	    }
	    x = x * x;
	    y >>= 1;
	}
	{
	    long xz = x * z;
	    if (!POSFIXABLE(xz) || xz / x != z) {
		goto bignum;
	    }
	    z = xz;
	}
    } while (--y);
    if (neg) z = -z;
    return LONG2NUM(z);
}

/*
 *  call-seq:
 *    fix ** other         => Numeric
 *
 *  Raises <code>fix</code> to the <code>other</code> power, which may
 *  be negative or fractional.
 *
 *    2 ** 3      #=> 8
 *    2 ** -1     #=> 0.5
 *    2 ** 0.5    #=> 1.4142135623731
 */

static VALUE
fix_pow(VALUE x, VALUE y)
{
    static const double zero = 0.0;
    long a = FIX2LONG(x);

    if (FIXNUM_P(y)) {
	long b = FIX2LONG(y);

	if (b < 0)
	  return rb_funcall(rb_rational_raw1(x), rb_intern("**"), 1, y);

	if (b == 0) return INT2FIX(1);
	if (b == 1) return x;
	if (a == 0) {
	    if (b > 0) return INT2FIX(0);
	    return DOUBLE2NUM(1.0 / zero);
	}
	if (a == 1) return INT2FIX(1);
	if (a == -1) {
	    if (b % 2 == 0)
		return INT2FIX(1);
	    else 
		return INT2FIX(-1);
	}
	return int_pow(a, b);
    }
    switch (TYPE(y)) {
      case T_BIGNUM:

	if (rb_funcall(y, '<', 1, INT2FIX(0)))
	  return rb_funcall(rb_rational_raw1(x), rb_intern("**"), 1, y);

	if (a == 0) return INT2FIX(0);
	if (a == 1) return INT2FIX(1);
	if (a == -1) {
	    if (int_even_p(y)) return INT2FIX(1);
	    else return INT2FIX(-1);
	}
	x = rb_int2big(FIX2LONG(x));
	return rb_big_pow(x, y);
      case T_FLOAT:
	if (RFLOAT_VALUE(y) == 0.0) return DOUBLE2NUM(1.0);
	if (a == 0) {
	    return DOUBLE2NUM(RFLOAT_VALUE(y) < 0 ? (1.0 / zero) : 0.0);
	}
	if (a == 1) return DOUBLE2NUM(1.0);
	return DOUBLE2NUM(pow((double)a, RFLOAT_VALUE(y)));
      default:
	return rb_num_coerce_bin(x, y, rb_intern("**"));
    }
}

/*
 * call-seq:
 *   fix == other
 *
 * Return <code>true</code> if <code>fix</code> equals <code>other</code>
 * numerically.
 *
 *   1 == 2      #=> false
 *   1 == 1.0    #=> true
 */

static VALUE
fix_equal(VALUE x, VALUE y)
{
    if (x == y) return Qtrue;
    if (FIXNUM_P(y)) return Qfalse;
    switch (TYPE(y)) {
      case T_BIGNUM:
	return rb_big_eq(y, x);
      case T_FLOAT:
	return (double)FIX2LONG(x) == RFLOAT_VALUE(y) ? Qtrue : Qfalse;
      default:
	return num_equal(x, y);
    }
}

/*
 *  call-seq:
 *     fix <=> numeric    => -1, 0, +1
 *
 *  Comparison---Returns -1, 0, or +1 depending on whether <i>fix</i> is
 *  less than, equal to, or greater than <i>numeric</i>. This is the
 *  basis for the tests in <code>Comparable</code>.
 */

static VALUE
fix_cmp(VALUE x, VALUE y)
{
    if (x == y) return INT2FIX(0);
    if (FIXNUM_P(y)) {
	if (FIX2LONG(x) > FIX2LONG(y)) return INT2FIX(1);
	return INT2FIX(-1);
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return rb_big_cmp(rb_int2big(FIX2LONG(x)), y);
      case T_FLOAT:
	return rb_dbl_cmp((double)FIX2LONG(x), RFLOAT_VALUE(y));
      default:
	return rb_num_coerce_cmp(x, y, rb_intern("<=>"));
    }
}

/*
 * call-seq:
 *   fix > other     => true or false
 *
 * Returns <code>true</code> if the value of <code>fix</code> is
 * greater than that of <code>other</code>.
 */

static VALUE
fix_gt(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	if (FIX2LONG(x) > FIX2LONG(y)) return Qtrue;
	return Qfalse;
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return FIX2INT(rb_big_cmp(rb_int2big(FIX2LONG(x)), y)) > 0 ? Qtrue : Qfalse;
      case T_FLOAT:
	return (double)FIX2LONG(x) > RFLOAT_VALUE(y) ? Qtrue : Qfalse;
      default:
	return rb_num_coerce_relop(x, y, '>');
    }
}

/*
 * call-seq:
 *   fix >= other     => true or false
 *
 * Returns <code>true</code> if the value of <code>fix</code> is
 * greater than or equal to that of <code>other</code>.
 */

static VALUE
fix_ge(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	if (FIX2LONG(x) >= FIX2LONG(y)) return Qtrue;
	return Qfalse;
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return FIX2INT(rb_big_cmp(rb_int2big(FIX2LONG(x)), y)) >= 0 ? Qtrue : Qfalse;
      case T_FLOAT:
	return (double)FIX2LONG(x) >= RFLOAT_VALUE(y) ? Qtrue : Qfalse;
      default:
	return rb_num_coerce_relop(x, y, rb_intern(">="));
    }
}

/*
 * call-seq:
 *   fix < other     => true or false
 *
 * Returns <code>true</code> if the value of <code>fix</code> is
 * less than that of <code>other</code>.
 */

static VALUE
fix_lt(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	if (FIX2LONG(x) < FIX2LONG(y)) return Qtrue;
	return Qfalse;
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return FIX2INT(rb_big_cmp(rb_int2big(FIX2LONG(x)), y)) < 0 ? Qtrue : Qfalse;
      case T_FLOAT:
	return (double)FIX2LONG(x) < RFLOAT_VALUE(y) ? Qtrue : Qfalse;
      default:
	return rb_num_coerce_relop(x, y, '<');
    }
}

/*
 * call-seq:
 *   fix <= other     => true or false
 *
 * Returns <code>true</code> if the value of <code>fix</code> is
 * less than or equal to that of <code>other</code>.
 */

static VALUE
fix_le(VALUE x, VALUE y)
{
    if (FIXNUM_P(y)) {
	if (FIX2LONG(x) <= FIX2LONG(y)) return Qtrue;
	return Qfalse;
    }
    switch (TYPE(y)) {
      case T_BIGNUM:
	return FIX2INT(rb_big_cmp(rb_int2big(FIX2LONG(x)), y)) <= 0 ? Qtrue : Qfalse;
      case T_FLOAT:
	return (double)FIX2LONG(x) <= RFLOAT_VALUE(y) ? Qtrue : Qfalse;
      default:
	return rb_num_coerce_relop(x, y, rb_intern("<="));
    }
}

/*
 * call-seq:
 *   ~fix     => integer
 *
 * One's complement: returns a number where each bit is flipped.
 */

static VALUE
fix_rev(VALUE num)
{
    long val = FIX2LONG(num);

    val = ~val;
    return LONG2NUM(val);
}

static VALUE
bit_coerce(VALUE x)
{
    while (!FIXNUM_P(x) && TYPE(x) != T_BIGNUM) {
	if (TYPE(x) == T_FLOAT) {
	    rb_raise(rb_eTypeError, "can't convert Float into Integer");
	}
	x = rb_to_int(x);
    }
    return x;
}

/*
 * call-seq:
 *   fix & other     => integer
 *
 * Bitwise AND.
 */

static VALUE
fix_and(VALUE x, VALUE y)
{
    long val;

    if (!FIXNUM_P(y = bit_coerce(y))) {
	return rb_big_and(y, x);
    }
    val = FIX2LONG(x) & FIX2LONG(y);
    return LONG2NUM(val);
}

/*
 * call-seq:
 *   fix | other     => integer
 *
 * Bitwise OR.
 */

static VALUE
fix_or(VALUE x, VALUE y)
{
    long val;

    if (!FIXNUM_P(y = bit_coerce(y))) {
	return rb_big_or(y, x);
    }
    val = FIX2LONG(x) | FIX2LONG(y);
    return LONG2NUM(val);
}

/*
 * call-seq:
 *   fix ^ other     => integer
 *
 * Bitwise EXCLUSIVE OR.
 */

static VALUE
fix_xor(VALUE x, VALUE y)
{
    long val;

    if (!FIXNUM_P(y = bit_coerce(y))) {
	return rb_big_xor(y, x);
    }
    val = FIX2LONG(x) ^ FIX2LONG(y);
    return LONG2NUM(val);
}

static VALUE fix_lshift(long, unsigned long);
static VALUE fix_rshift(long, unsigned long);

/*
 * call-seq:
 *   fix << count     => integer
 *
 * Shifts _fix_ left _count_ positions (right if _count_ is negative).
 */

static VALUE
rb_fix_lshift(VALUE x, VALUE y)
{
    long val, width;

    val = NUM2LONG(x);
    if (!FIXNUM_P(y))
	return rb_big_lshift(rb_int2big(val), y);
    width = FIX2LONG(y);
    if (width < 0)
	return fix_rshift(val, (unsigned long)-width);
    return fix_lshift(val, width);
}

static VALUE
fix_lshift(long val, unsigned long width)
{
    if (width > (SIZEOF_LONG*CHAR_BIT-1)
	|| ((unsigned long)val)>>(SIZEOF_LONG*CHAR_BIT-1-width) > 0) {
	return rb_big_lshift(rb_int2big(val), ULONG2NUM(width));
    }
    val = val << width;
    return LONG2NUM(val);
}

/*
 * call-seq:
 *   fix >> count     => integer
 *
 * Shifts _fix_ right _count_ positions (left if _count_ is negative).
 */

static VALUE
rb_fix_rshift(VALUE x, VALUE y)
{
    long i, val;

    val = FIX2LONG(x);
    if (!FIXNUM_P(y))
	return rb_big_rshift(rb_int2big(val), y);
    i = FIX2LONG(y);
    if (i == 0) return x;
    if (i < 0)
	return fix_lshift(val, (unsigned long)-i);
    return fix_rshift(val, i);
}

static VALUE
fix_rshift(long val, unsigned long i)
{
    if (i >= sizeof(long)*CHAR_BIT-1) {
	if (val < 0) return INT2FIX(-1);
	return INT2FIX(0);
    }
    val = RSHIFT(val, i);
    return LONG2FIX(val);
}

/*
 *  call-seq:
 *     fix[n]     => 0, 1
 *
 *  Bit Reference---Returns the <em>n</em>th bit in the binary
 *  representation of <i>fix</i>, where <i>fix</i>[0] is the least
 *  significant bit.
 *
 *     a = 0b11001100101010
 *     30.downto(0) do |n| print a[n] end
 *
 *  <em>produces:</em>
 *
 *     0000000000000000011001100101010
 */

static VALUE
fix_aref(VALUE fix, VALUE idx)
{
    long val = FIX2LONG(fix);
    long i;

    idx = rb_to_int(idx);
    if (!FIXNUM_P(idx)) {
	idx = rb_big_norm(idx);
	if (!FIXNUM_P(idx)) {
	    if (!RBIGNUM_SIGN(idx) || val >= 0)
		return INT2FIX(0);
	    return INT2FIX(1);
	}
    }
    i = FIX2LONG(idx);

    if (i < 0) return INT2FIX(0);
    if (SIZEOF_LONG*CHAR_BIT-1 < i) {
	if (val < 0) return INT2FIX(1);
	return INT2FIX(0);
    }
    if (val & (1L<<i))
	return INT2FIX(1);
    return INT2FIX(0);
}

/*
 *  call-seq:
 *     fix.to_f -> float
 *
 *  Converts <i>fix</i> to a <code>Float</code>.
 *
 */

static VALUE
fix_to_f(VALUE num)
{
    double val;

    val = (double)FIX2LONG(num);

    return DOUBLE2NUM(val);
}

/*
 *  call-seq:
 *     fix.abs -> aFixnum
 *
 *  Returns the absolute value of <i>fix</i>.
 *
 *     -12345.abs   #=> 12345
 *     12345.abs    #=> 12345
 *
 */

static VALUE
fix_abs(VALUE fix)
{
    long i = FIX2LONG(fix);

    if (i < 0) i = -i;

    return LONG2NUM(i);
}



/*
 *  call-seq:
 *     fix.size -> fixnum
 *
 *  Returns the number of <em>bytes</em> in the machine representation
 *  of a <code>Fixnum</code>.
 *
 *     1.size            #=> 4
 *     -1.size           #=> 4
 *     2147483647.size   #=> 4
 */

static VALUE
fix_size(VALUE fix)
{
    return INT2FIX(sizeof(long));
}

/*
 *  call-seq:
 *     int.upto(limit) {|i| block }     => int
 *
 *  Iterates <em>block</em>, passing in integer values from <i>int</i>
 *  up to and including <i>limit</i>.
 *
 *     5.upto(10) { |i| print i, " " }
 *
 *  <em>produces:</em>
 *
 *     5 6 7 8 9 10
 */

static VALUE
int_upto(VALUE from, VALUE to)
{
    RETURN_ENUMERATOR(from, 1, &to);
    if (FIXNUM_P(from) && FIXNUM_P(to)) {
	long i, end;

	end = FIX2LONG(to);
	for (i = FIX2LONG(from); i <= end; i++) {
	    rb_yield(LONG2FIX(i));
	}
    }
    else {
	VALUE i = from, c;

	while (!(c = rb_funcall(i, '>', 1, to))) {
	    rb_yield(i);
	    i = rb_funcall(i, '+', 1, INT2FIX(1));
	}
	if (NIL_P(c)) rb_cmperr(i, to);
    }
    return from;
}

/*
 *  call-seq:
 *     int.downto(limit) {|i| block }     => int
 *
 *  Iterates <em>block</em>, passing decreasing values from <i>int</i>
 *  down to and including <i>limit</i>.
 *
 *     5.downto(1) { |n| print n, ".. " }
 *     print "  Liftoff!\n"
 *
 *  <em>produces:</em>
 *
 *     5.. 4.. 3.. 2.. 1..   Liftoff!
 */

static VALUE
int_downto(VALUE from, VALUE to)
{
    RETURN_ENUMERATOR(from, 1, &to);
    if (FIXNUM_P(from) && FIXNUM_P(to)) {
	long i, end;

	end = FIX2LONG(to);
	for (i=FIX2LONG(from); i >= end; i--) {
	    rb_yield(LONG2FIX(i));
	}
    }
    else {
	VALUE i = from, c;

	while (!(c = rb_funcall(i, '<', 1, to))) {
	    rb_yield(i);
	    i = rb_funcall(i, '-', 1, INT2FIX(1));
	}
	if (NIL_P(c)) rb_cmperr(i, to);
    }
    return from;
}

/*
 *  call-seq:
 *     int.times {|i| block }     => int
 *
 *  Iterates block <i>int</i> times, passing in values from zero to
 *  <i>int</i> - 1.
 *
 *     5.times do |i|
 *       print i, " "
 *     end
 *
 *  <em>produces:</em>
 *
 *     0 1 2 3 4
 */

static VALUE
int_dotimes(VALUE num)
{
    RETURN_ENUMERATOR(num, 0, 0);

    if (FIXNUM_P(num)) {
	long i, end;

	end = FIX2LONG(num);
	for (i=0; i<end; i++) {
	    rb_yield(LONG2FIX(i));
	}
    }
    else {
	VALUE i = INT2FIX(0);

	for (;;) {
	    if (!RTEST(rb_funcall(i, '<', 1, num))) break;
	    rb_yield(i);
	    i = rb_funcall(i, '+', 1, INT2FIX(1));
	}
    }
    return num;
}

static VALUE
int_round(int argc, VALUE* argv, VALUE num)
{
    VALUE n, f, h, r;
    int ndigits;

    if (argc == 0) return num;
    rb_scan_args(argc, argv, "1", &n);
    ndigits = NUM2INT(n);
    if (ndigits > 0) {
	return rb_Float(num);
    }
    if (ndigits == 0) {
	return num;
    }
    ndigits = -ndigits;
    if (ndigits < 0) {
	rb_raise(rb_eArgError, "ndigits out of range");
    }
    f = int_pow(10, ndigits);
    if (FIXNUM_P(num) && FIXNUM_P(f)) {
	SIGNED_VALUE x = FIX2LONG(num), y = FIX2LONG(f);
	int neg = x < 0;
	if (neg) x = -x;
	x = (x + y / 2) / y * y;
	if (neg) x = -x;
	return LONG2NUM(x);
    }
    h = rb_funcall(f, '/', 1, INT2FIX(2));
    r = rb_funcall(num, '%', 1, f);
    n = rb_funcall(num, '-', 1, r);
    if (!RTEST(rb_funcall(r, '<', 1, h))) {
	n = rb_funcall(n, '+', 1, f);
    }
    return n;
}

/*
 *  call-seq:
 *     fix.zero?    => true or false
 *
 *  Returns <code>true</code> if <i>fix</i> is zero.
 *
 */

static VALUE
fix_zero_p(VALUE num)
{
    if (FIX2LONG(num) == 0) {
	return Qtrue;
    }
    return Qfalse;
}

/*
 *  call-seq:
 *     fix.odd? -> true or false
 *
 *  Returns <code>true</code> if <i>fix</i> is an odd number.
 */

static VALUE
fix_odd_p(VALUE num)
{
    if (num & 2) {
	return Qtrue;
    }
    return Qfalse;
}

/*
 *  call-seq:
 *     fix.even? -> true or false
 *
 *  Returns <code>true</code> if <i>fix</i> is an even number.
 */

static VALUE
fix_even_p(VALUE num)
{
    if (num & 2) {
	return Qfalse;
    }
    return Qtrue;
}

#if WITH_OBJC
static const char *
imp_rb_float_objCType(void *rcv, SEL sel)
{
    return "d";
}

static const char *
imp_rb_fixnum_objCType(void *rcv, SEL sel)
{
    return "l";
}

static void
imp_rb_float_getValue(void *rcv, SEL sel, void *buffer)
{
    double v = RFLOAT_VALUE(rcv);
    *(double *)buffer = v;
}

static void
imp_rb_fixnum_getValue(void *rcv, SEL sel, void *buffer)
{
    long v = RFIXNUM(rcv)->value;
    *(long *)buffer = v;
}

static double
imp_rb_float_doubleValue(void *rcv, SEL sel)
{
    return RFLOAT_VALUE(rcv);
}

static long long
imp_rb_fixnum_longValue(void *rcv, SEL sel)
{
    return RFIXNUM(rcv)->value;
}

static void
rb_install_nsnumber_primitives(void)
{
    Class klass;

    klass = (Class)rb_cFloat;
    rb_objc_install_method2(klass, "objCType",
	    (IMP)imp_rb_float_objCType);
    rb_objc_install_method2(klass, "getValue:", 
	    (IMP)imp_rb_float_getValue);
    rb_objc_install_method2(klass, "doubleValue", 
	    (IMP)imp_rb_float_doubleValue);

    klass = (Class)rb_cFixnum;
    rb_objc_install_method2(klass, "objCType",
	    (IMP)imp_rb_fixnum_objCType);
    rb_objc_install_method2(klass, "getValue:", 
	    (IMP)imp_rb_fixnum_getValue);
    rb_objc_install_method2(klass, "longValue",
	    (IMP)imp_rb_fixnum_longValue);
}
#endif

static inline double
nsnum_cdouble(VALUE num)
{
    double val; 
    CFNumberGetValue((CFNumberRef)num, kCFNumberDoubleType, &val);
    return val;
}

static inline long long
nsnum_cLL(VALUE num)
{
    long long val; 
    CFNumberGetValue((CFNumberRef)num, kCFNumberLongLongType, &val);
    return val;
}

static VALUE
nsnum_to_i(VALUE num)
{
    return LL2NUM(nsnum_cLL(num));
}

static VALUE
nsnum_to_f(VALUE num)
{
    return DOUBLE2NUM(nsnum_cdouble(num));
}

static VALUE
nsnum_class(VALUE num)
{
    CFNumberType t = CFNumberGetType((CFNumberRef)num);
    switch (t) {
	case kCFNumberSInt8Type:
	case kCFNumberSInt16Type:
	case kCFNumberSInt32Type:
	case kCFNumberSInt64Type:
	case kCFNumberCharType:
	case kCFNumberShortType:
	case kCFNumberIntType:
	case kCFNumberLongType:
	case kCFNumberLongLongType:
	case kCFNumberCFIndexType:
	case kCFNumberNSIntegerType:
	    return rb_cFixnum;
	case kCFNumberFloat32Type:
	case kCFNumberFloat64Type:
	case kCFNumberFloatType:
	case kCFNumberDoubleType:
	case kCFNumberMaxType:
	    return rb_cFloat;
    }
    if (CFNumberIsFloatType((CFNumberRef)num)) return rb_cFloat;
    return rb_cNSNumber; //This could be something exotic like a struct
}

static VALUE
nsnum_to_rb(VALUE num)
{
    if (FIXNUM_P(num)) {
	return num;
    }
    VALUE c = CLASS_OF(num);
    if (c != rb_cCFNumber && c != rb_cNSNumber) {
	return num;
    }
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFloat) {
	return DOUBLE2NUM(nsnum_cdouble(num));
    }
    if (num_class == rb_cFixnum) {
	return LL2NUM(nsnum_cLL(num));
    }
    return num;
}

static void
nsnum_fail_coerce(VALUE num)
{
    VALUE nsn_desc = rb_funcall(num, rb_intern("description"), 0);
    const char* numdesc = StringValueCStr(nsn_desc);
    rb_raise(rb_eTypeError, "Cannot coerce NSNumber %s into a ruby number", numdesc);
}

static VALUE
nsnum_to_rb_force(VALUE num)
{
    if (FIXNUM_P(num)) {
	return num;
    }
    VALUE c = CLASS_OF(num);
    if (c != rb_cCFNumber && c != rb_cNSNumber) {
	return num;
    }
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFloat) {
	return DOUBLE2NUM((nsnum_cdouble(num)));
    }
    if (num_class == rb_cFixnum) {
	return LL2NUM((nsnum_cLL(num)));
    }
    nsnum_fail_coerce(num);
    return Qnil;
}

static VALUE
nsnum_to_rb_abs(VALUE num)
{
    VALUE num_class = nsnum_class(num);
    if (num_class==rb_cFloat) {
	return DOUBLE2NUM(fabs(nsnum_cdouble(num)));
    }
    if (num_class==rb_cFixnum) {
	return LL2NUM(llabs(nsnum_cLL(num)));
    }
    nsnum_fail_coerce(num);
    return Qnil;
}

static VALUE
nsnum_to_rb_neg(VALUE num)
{
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFloat) {
	return DOUBLE2NUM(-(nsnum_cdouble(num)));
    }
    if (num_class == rb_cFixnum) {
	return LL2NUM(-(nsnum_cLL(num)));
    }
    nsnum_fail_coerce(num);
    return Qnil;
}

static VALUE
nsnum_cmp(VALUE l, VALUE r)
{
    if (!do_coerce(&l, &r, Qtrue)) {
	return Qnil;
    }
    return rb_funcall(l, id_spaceship, 1, r);
}

static VALUE
nsnum_eq(VALUE l, VALUE r)
{
    if (!do_coerce(&l, &r, Qtrue)) {
	return Qnil;
    }
    return rb_funcall(l, id_eq, 1, r);
}

static VALUE
nsnum_coerce(VALUE num, VALUE b)
{
    return num_coerce(nsnum_to_rb_force(num),b);
}

static VALUE
nsnum_init_copy(VALUE x)
{
    /* Numerics are immutable values, which should not be copied */
    rb_raise(rb_eTypeError, "can't copy %s",rb_class2name(nsnum_class(x)));
    return Qnil;		/* not reached */
}

static VALUE
nsnum_dup(VALUE x)
{
    /* Numerics are immutable values, which should not be copied */
    rb_raise(rb_eTypeError, "can't dup %s", rb_class2name(nsnum_class(x)));
    return Qnil;		/* not reached */
}

/*static VALUE
  nsnum_missing(int argc, VALUE* argv, VALUE self)
  {
  VALUE rb_self = nsnum_to_rb(self);
  rb_p(rb_self);
  static ID to_s = Qnil;
  if (to_s==Qnil) to_s = rb_intern("to_s");
  printf("to_s: %s\n", rb_id2name(to_s));
  VALUE sel_name = rb_funcall(argv[0], to_s, 0);
  rb_p(sel_name);
  ID sel = rb_to_id(sel_name);
  printf("sel: %s\n", rb_id2name(sel));
  if (rb_respond_to(rb_self, sel)) {
  return rb_self;//rb_funcall2(rb_self, sel, argc-1, argv+1);
  } else {
  rb_raise(rb_eNoMethodError, "undefined method `%s' for %s", rb_id2name(sel), rb_obj_classname(rb_self));
  }
  return Qnil;
  }*/

static VALUE
nsnum_quo(VALUE a, VALUE b)
{
    static ID sel = 0;
    if (!sel) {
	sel = rb_intern("quo");
    }
    do_coerce(&a,&b,Qtrue);
    return rb_funcall(a, sel, 1, b);
}

static VALUE
nsnum_fdiv(VALUE a, VALUE b)
{
    static ID sel = 0;
    if (!sel) {
	sel = rb_intern("fdiv");
    }
    do_coerce(&a,&b,Qtrue);
    return rb_funcall(a, sel, 1, b);
}

static VALUE
nsnum_divmod(VALUE a, VALUE b)
{
    static ID sel = 0;
    if (!sel) {
	sel = rb_intern("divmod");
    }
    do_coerce(&a,&b,Qtrue);
    return rb_funcall(a, sel, 1, b);
}

static VALUE
nsnum_modulo(VALUE a, VALUE b)
{
    static ID sel = 0;
    if (!sel) {
	sel = rb_intern("modulo");
    }
    do_coerce(&a,&b,Qtrue);
    return rb_funcall(a, sel, 1, b);
}

static VALUE
nsnum_remainder(VALUE a, VALUE b)
{
    static ID sel = 0;
    if (!sel) {
	sel = rb_intern("remainder");
    }
    do_coerce(&a,&b,Qtrue);
    return rb_funcall(a, sel, 1, b);
}

static VALUE
nsnum_true(VALUE a)
{
    return Qtrue;
}

static VALUE
nsnum_integer_p(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	return Qfalse;
    }
    if (num_class == rb_cFixnum) {
	return Qtrue;
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_zero_p(VALUE num)
{
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFloat) {
	return nsnum_cdouble(num) == 0;
    }
    if (num_class == rb_cFixnum) {
	return nsnum_cLL(num) == 0;
    }
    nsnum_fail_coerce(num);
    return Qnil;
}

static VALUE
nsnum_nonzero_p(VALUE num)
{
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFloat) {
	return nsnum_cdouble(num) != 0;
    }
    if (num_class == rb_cFixnum) {
	return nsnum_cLL(num) != 0;
    }
    nsnum_fail_coerce(num);
    return Qnil;
}

static VALUE
nsnum_floor(VALUE num)
{
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFixnum) {
	return num;
    }
    if (num_class != rb_cFloat) {
	nsnum_fail_coerce(num);
	return Qnil;
    }
    double v = floor(nsnum_cdouble(num));
    if (!FIXABLE(v)) {
	return rb_dbl2big(v);
    }
    return LL2NUM((long long)v);
}

static VALUE
nsnum_ceil(VALUE num)
{
    VALUE num_class = nsnum_class(num);
    if (num_class == rb_cFixnum) {
	return num;
    }
    if (num_class != rb_cFloat) {
	nsnum_fail_coerce(num);
	return Qnil;
    }
    double v = ceil(nsnum_cdouble(num));
    if (!FIXABLE(v)) {
	return rb_dbl2big(v);
    }
    return LL2NUM((long long)v);
}

static VALUE
nsnum_round(int argc, VALUE* argv, VALUE self)
{
    VALUE num_class = nsnum_class(self);
    if (num_class == rb_cFloat) {
	return flo_round(argc, argv, DOUBLE2NUM(nsnum_cdouble(self)));
    }
    if (num_class == rb_cFixnum) {
	return int_round(argc, argv, LL2NUM(nsnum_cLL(self)));
    }
    nsnum_fail_coerce(self);
    return Qnil;
}

static VALUE
nsnum_truncate(VALUE self)
{
    VALUE num_class = nsnum_class(self);
    if (num_class == rb_cFloat) {
	return LL2NUM(nsnum_cdouble(self));
    }
    if (num_class == rb_cFixnum) {
	return self;
    }
    nsnum_fail_coerce(self);
    return Qnil;
}

static VALUE
nsnum_step(int argc, VALUE* argv, VALUE self)
{
    VALUE num_class = nsnum_class(self);
    if (num_class == rb_cFloat) {
	return num_step(argc, argv, DOUBLE2NUM(nsnum_cdouble(self)));
    }
    if (num_class == rb_cFixnum) {
	return num_step(argc, argv, LL2NUM(nsnum_cLL(self)));
    }
    nsnum_fail_coerce(self);
    return Qnil;
}

static VALUE
nsnum_numerator(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	return num_numerator(DOUBLE2NUM(nsnum_cdouble(a)));
    }
    if (num_class == rb_cFixnum) {
	return LL2NUM(nsnum_cLL(a));
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_denominator(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	return num_denominator(DOUBLE2NUM(nsnum_cdouble(a)));
    }
    if (num_class == rb_cFixnum) {
	return LONG2FIX(1);
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

    static VALUE
nsnum_even_p(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	//TODO: it will still respond_to the method even if it shouldn't
	rb_raise(rb_eNoMethodError, "undefined method `even?' for %s", rb_obj_classname(a));
    }
    if (num_class == rb_cFixnum) {
	return (nsnum_cLL(a) & 1) ? Qfalse : Qtrue;
    }
    return Qnil;
}

static VALUE
nsnum_odd_p(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	//TODO: it will still respond_to the method even if it shouldn't
	rb_raise(rb_eNoMethodError, "undefined method `odd?' for %s", rb_obj_classname(a));
    }
    if (num_class == rb_cFixnum) {
	return (nsnum_cLL(a) & 1) ? Qtrue : Qfalse;
    }
    return Qnil;
}

static VALUE
nsnum_upto(VALUE a, VALUE b)
{
    if (FIXNUM_P(b)) {
	long i = nsnum_cLL(a);
	long j = FIX2LONG(b);
	while (i <= j) {
	    rb_yield(LONG2FIX(i));
	    i++;
	}
	return a;
    }
    VALUE i = a;
    VALUE c;
    while (1) {
	c = rb_funcall(i, '>', 1, b);
	if (c == Qnil || c == Qtrue) {
	    break;
	}
	rb_yield(i);
	i = rb_funcall(i, '+', 1, INT2FIX(1));
    }
    if (NIL_P(c)) {
	rb_cmperr(i, b);
    }
    return a;
}

static VALUE
nsnum_downto(VALUE a, VALUE b)
{
    long long i = nsnum_cLL(a);
    if (FIXNUM_P(b) && FIXABLE(i)) {
	long j = FIX2LONG(b);
	while (i >= j) {
	    rb_yield(LONG2FIX(i));
	    i--;
	}
	return a;
    }
    VALUE ii = a;
    VALUE c;
    while (1) {
	c = rb_funcall(ii, '<', 1, b);
	if (c == Qnil || c == Qtrue) {
	    break;
	}
	rb_yield(ii);
	ii = rb_funcall(ii, '-', 1, INT2FIX(1));
    }
    if (NIL_P(c)) {
	rb_cmperr(ii, b);
    }
    return a;
}

static VALUE
nsnum_dotimes(VALUE a)
{
    long long j = nsnum_cLL(a);
    long long i;
    for (i = 0; i < j; i++) {
	rb_yield(LL2NUM(i));
    }
    return a;
}

static VALUE
nsnum_succ(VALUE a)
{
    long long j = nsnum_cLL(a);
    if (j + 1 < j) {
	VALUE r = LL2NUM(j);
	return rb_funcall(r, '+', 1, INT2FIX(1));
    }
    return LL2NUM(j + 1);
}

static VALUE
nsnum_pred(VALUE a)
{
    long long j = nsnum_cLL(a);
    if (j - 1 > j) {
	VALUE r = LL2NUM(j);
	return rb_funcall(r, '-', 1, INT2FIX(1));
    }
    return LL2NUM(j - 1);
}

static VALUE
nsnum_nan_p(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	return isnan(nsnum_cdouble(a)) ? Qtrue : Qfalse;
    }
    if (num_class == rb_cFixnum) {
	//TODO: it will still respond_to the method even if it shouldn't
	rb_raise(rb_eNoMethodError, "undefined method `is_nan?' for %s", rb_obj_classname(a));
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_inf_p(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	return isinf(nsnum_cdouble(a)) ? Qtrue : Qfalse;
    }
    if (num_class == rb_cFixnum) {
	//TODO: it will still respond_to the method even if it shouldn't
	rb_raise(rb_eNoMethodError, "undefined method `is_nan?' for %s", rb_obj_classname(a));
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_notinf_p(VALUE a)
{
    VALUE num_class = nsnum_class(a);
    if (num_class == rb_cFloat) {
	return isinf(nsnum_cdouble(a)) ? Qfalse : Qtrue;
    }
    if (num_class == rb_cFixnum) {
	//TODO: it will still respond_to the method even if it shouldn't
	rb_raise(rb_eNoMethodError, "undefined method `is_nan?' for %s", rb_obj_classname(a));
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_pow(VALUE a, VALUE b)
{
    VALUE num_class = nsnum_class(a);
    b = nsnum_to_rb_force(b);
    if (num_class == rb_cFloat) {
	return flo_pow(DOUBLE2NUM(nsnum_cdouble(a)), b);
    }
    if (num_class == rb_cFixnum) {
	return fix_pow(LL2NUM(nsnum_cLL(a)), b);
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_plus(VALUE a, VALUE b)
{
    VALUE num_class = nsnum_class(a);
    b = nsnum_to_rb_force(b);
    if (num_class == rb_cFloat) {
	return flo_plus(DOUBLE2NUM(nsnum_cdouble(a)), b);
    }
    if (num_class == rb_cFixnum) {
	return fix_plus(LL2NUM(nsnum_cLL(a)), b);
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_minus(VALUE a, VALUE b)
{
    VALUE num_class = nsnum_class(a);
    b = nsnum_to_rb_force(b);
    if (num_class == rb_cFloat) {
	return flo_minus(DOUBLE2NUM(nsnum_cdouble(a)), b);
    }
    if (num_class == rb_cFixnum) {
	return fix_minus(LL2NUM(nsnum_cLL(a)), b);
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_times(VALUE a, VALUE b)
{
    VALUE num_class = nsnum_class(a);
    b = nsnum_to_rb_force(b);
    if (num_class == rb_cFloat) {
	return flo_mul(DOUBLE2NUM(nsnum_cdouble(a)), b);
    }
    if (num_class == rb_cFixnum) {
	return fix_mul(LL2NUM(nsnum_cLL(a)), b);
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_div(VALUE a, VALUE b)
{
    VALUE num_class = nsnum_class(a);
    b = nsnum_to_rb_force(b);
    if (num_class == rb_cFloat) {
	return flo_div(DOUBLE2NUM(nsnum_cdouble(a)), b);
    }
    if (num_class == rb_cFixnum) {
	return fix_div(LL2NUM(nsnum_cLL(a)), b);
    }
    nsnum_fail_coerce(a);
    return Qnil;
}

static VALUE
nsnum_rev(VALUE a)
{
    return LONG2FIX(~FIX2LONG(a));
}

static VALUE
nsnum_and(VALUE a, VALUE b)
{
    b = bit_coerce(nsnum_to_rb_force(b));
    if (!FIXNUM_P(b)) {
	return rb_big_and(nsnum_to_rb_force(a), b);
    }
    return LONG2NUM(nsnum_cLL(a) & FIX2LONG(b));
}

static VALUE
nsnum_or(VALUE a, VALUE b)
{
    b = bit_coerce(nsnum_to_rb_force(b));
    if (!FIXNUM_P(b)) {
	return rb_big_or(nsnum_to_rb_force(a), b);
    }
    return LONG2NUM(nsnum_cLL(a) | FIX2LONG(b));
}

static VALUE
nsnum_xor(VALUE a, VALUE b)
{
    b = bit_coerce(nsnum_to_rb_force(b));
    if (!FIXNUM_P(b)) {
	return rb_big_xor(nsnum_to_rb_force(a), b);
    }
    return LONG2NUM(nsnum_cLL(a) ^ FIX2LONG(b));
}

static VALUE
nsnum_aref(VALUE l, VALUE idx)
{
    long long v = nsnum_cLL(l);
    idx = rb_to_int(idx);
    if (!FIXNUM_P(idx)) {
	idx = rb_big_norm(idx);
	if (!FIXNUM_P(idx)) {
	    if (!RBIGNUM_SIGN(idx) || v >= 0) {
		return INT2FIX(0);
	    }
	    return INT2FIX(1);
	}
    }
    long i = FIX2LONG(idx);
    if (i < 0) return INT2FIX(0);
    if (SIZEOF_LONG * CHAR_BIT - 1 < i) {
	if (v < 0) {
	    return INT2FIX(1);
	}
	return INT2FIX(0);
    }
    if (v & (1L << i)) {
	return INT2FIX(1);
    }
    return INT2FIX(0);
}

static VALUE
nsnum_shl(VALUE v, VALUE amt)
{
    VALUE num_class = nsnum_class(v);
    if (num_class == rb_cFloat) {
	rb_raise(rb_eNoMethodError, "undefined method `<<' for %s", rb_obj_classname(v));
    }
    return rb_funcall(nsnum_to_rb_force(v), id_shl, 1, nsnum_to_rb_force(amt));
}

static VALUE
nsnum_shr(VALUE v, VALUE amt)
{
    VALUE num_class = nsnum_class(v);
    if (num_class == rb_cFloat) {
	rb_raise(rb_eNoMethodError, "undefined method `>>' for %s", rb_obj_classname(v));
    }
    return rb_funcall(nsnum_to_rb_force(v), id_shr, 1, nsnum_to_rb_force(amt));
}

static void
rb_install_nsnumber_methods() {
    id_spaceship = rb_intern("<=>");
    id_shl = rb_intern("<<");
    id_shr = rb_intern(">>");
    id_pow = rb_intern("**");

    rb_cNSNumber = (VALUE)objc_getClass("NSNumber");
    rb_define_method(rb_cNSNumber, "to_i", nsnum_to_i,0);
    rb_define_method(rb_cNSNumber, "to_f", nsnum_to_f,0);
    rb_define_method(rb_cNSNumber, "to_rb", nsnum_to_rb,0);

    //rb_define_method(rb_cNSNumber, "class", nsnum_class,0);
    rb_define_method(rb_cNSNumber, "dup", nsnum_dup,0);
    rb_define_method(rb_cNSNumber, "initialize_copy", nsnum_init_copy,0);
    rb_define_method(rb_cNSNumber, "coerce", nsnum_coerce, 1);

    rb_define_method(rb_cNSNumber, "+@", nsnum_to_rb, 0);
    rb_define_method(rb_cNSNumber, "-@", nsnum_to_rb_neg, 0);
    rb_define_method(rb_cNSNumber, "<=>", nsnum_cmp, 1);
    rb_define_method(rb_cNSNumber, "eql?", nsnum_eq, 1);
    rb_define_method(rb_cNSNumber, "quo", nsnum_quo, 1);
    rb_define_method(rb_cNSNumber, "fdiv", nsnum_fdiv, 1);
    rb_define_method(rb_cNSNumber, "div", nsnum_div, 1);
    rb_define_method(rb_cNSNumber, "divmod", nsnum_divmod, 1);
    rb_define_method(rb_cNSNumber, "modulo", nsnum_modulo, 1);
    rb_define_method(rb_cNSNumber, "%", nsnum_modulo, 1);
    rb_define_method(rb_cNSNumber, "remainder", nsnum_remainder, 1);
    rb_define_method(rb_cNSNumber, "abs", nsnum_to_rb_abs, 0);
    rb_define_method(rb_cNSNumber, "to_int", nsnum_to_i, 0);

    rb_define_method(rb_cNSNumber, "scalar?", nsnum_true, 0);
    rb_define_method(rb_cNSNumber, "integer?", nsnum_integer_p, 0);
    rb_define_method(rb_cNSNumber, "zero?", nsnum_zero_p, 0);
    rb_define_method(rb_cNSNumber, "nonzero?", nsnum_nonzero_p, 0);

    rb_define_method(rb_cNSNumber, "floor", nsnum_floor, 0);
    rb_define_method(rb_cNSNumber, "ceil", nsnum_ceil, 0);
    rb_define_method(rb_cNSNumber, "round", nsnum_round, -1);
    rb_define_method(rb_cNSNumber, "truncate", nsnum_truncate, 0);
    rb_define_method(rb_cNSNumber, "step", nsnum_step, -1);

    rb_define_method(rb_cNSNumber, "numerator", nsnum_numerator, 0);
    rb_define_method(rb_cNSNumber, "denominator", nsnum_denominator, 0);

    rb_define_method(rb_cNSNumber, "odd?", nsnum_odd_p, 0);
    rb_define_method(rb_cNSNumber, "even?", nsnum_even_p, 0);

    rb_define_method(rb_cNSNumber, "upto", nsnum_upto, 1);
    rb_define_method(rb_cNSNumber, "downto", nsnum_downto, 1);
    rb_define_method(rb_cNSNumber, "times", nsnum_dotimes, 0);	

    rb_define_method(rb_cNSNumber, "succ", nsnum_succ, 0);	
    rb_define_method(rb_cNSNumber, "next", nsnum_succ, 0);	
    rb_define_method(rb_cNSNumber, "pred", nsnum_pred, 0);	

    rb_define_method(rb_cNSNumber, "nan?",      nsnum_nan_p, 0);
    rb_define_method(rb_cNSNumber, "infinite?", nsnum_inf_p, 0);
    rb_define_method(rb_cNSNumber, "finite?",   nsnum_notinf_p, 0);	

    rb_define_method(rb_cNSNumber, "**", nsnum_pow, 1);
    rb_define_method(rb_cNSNumber, "+", nsnum_plus, 1);
    rb_define_method(rb_cNSNumber, "-", nsnum_minus, 1);
    rb_define_method(rb_cNSNumber, "*", nsnum_times, 1);
    rb_define_method(rb_cNSNumber, "/", nsnum_div, 1);

    rb_define_method(rb_cNSNumber, "~", nsnum_rev, 0);
    rb_define_method(rb_cNSNumber, "&", nsnum_and, 1);
    rb_define_method(rb_cNSNumber, "|", nsnum_or,  1);
    rb_define_method(rb_cNSNumber, "^", nsnum_xor, 1);
    rb_define_method(rb_cNSNumber, "[]", nsnum_aref, 1);
    rb_define_method(rb_cNSNumber, "<<", nsnum_shl, 1);
    rb_define_method(rb_cNSNumber, ">>", nsnum_shr, 1);

    //rb_define_method(rb_cNSNumber, "method_missing", nsnum_missing, -1);
    rb_include_module(rb_cNSNumber, rb_mComparable);
}

void
Init_Numeric(void)
{
#if defined(__FreeBSD__) && __FreeBSD__ < 4
    /* allow divide by zero -- Inf */
    fpsetmask(fpgetmask() & ~(FP_X_DZ|FP_X_INV|FP_X_OFL));
#elif defined(_UNICOSMP)
    /* Turn off floating point exceptions for divide by zero, etc. */
    _set_Creg(0, 0);
#elif defined(__BORLANDC__)
    /* Turn off floating point exceptions for overflow, etc. */
    _control87(MCW_EM, MCW_EM);
#endif
    id_coerce = rb_intern("coerce");
    id_to_i = rb_intern("to_i");
    id_eq = rb_intern("==");

    rb_eZeroDivError = rb_define_class("ZeroDivisionError", rb_eStandardError);
    rb_eFloatDomainError = rb_define_class("FloatDomainError", rb_eRangeError);
#if WITH_OBJC
    rb_cCFNumber = (VALUE)objc_getClass("NSCFNumber");
    rb_cNumeric = rb_define_class("Numeric", (VALUE)objc_getClass("NSNumber"));
    RCLASS_SET_VERSION_FLAG(rb_cNumeric, RCLASS_IS_OBJECT_SUBCLASS);
    rb_define_object_special_methods(rb_cNumeric);
    /* overriding NSObject methods */
    rb_define_method(rb_cNumeric, "class", rb_obj_class, 0);
    rb_define_method(rb_cNumeric, "dup", rb_obj_dup, 0);
#else
    rb_cNumeric = rb_define_class("Numeric", rb_cObject);
#endif

    rb_define_method(rb_cNumeric, "singleton_method_added", num_sadded, 1);
    rb_include_module(rb_cNumeric, rb_mComparable);
    rb_define_method(rb_cNumeric, "initialize_copy", num_init_copy, 1);
    rb_define_method(rb_cNumeric, "coerce", num_coerce, 1);

    rb_define_method(rb_cNumeric, "+@", num_uplus, 0);
    rb_define_method(rb_cNumeric, "-@", num_uminus, 0);
    rb_define_method(rb_cNumeric, "<=>", num_cmp, 1);
    rb_define_method(rb_cNumeric, "eql?", num_eql, 1);
    rb_define_method(rb_cNumeric, "quo", num_quo, 1);
    rb_define_method(rb_cNumeric, "fdiv", num_fdiv, 1);
    rb_define_method(rb_cNumeric, "div", num_div, 1);
    rb_define_method(rb_cNumeric, "divmod", num_divmod, 1);
    rb_define_method(rb_cNumeric, "modulo", num_modulo, 1);
    rb_define_method(rb_cNumeric, "remainder", num_remainder, 1);
    rb_define_method(rb_cNumeric, "abs", num_abs, 0);
    rb_define_method(rb_cNumeric, "to_int", num_to_int, 0);

    rb_define_method(rb_cNumeric, "scalar?", num_scalar_p, 0);
    rb_define_method(rb_cNumeric, "integer?", num_int_p, 0);
    rb_define_method(rb_cNumeric, "zero?", num_zero_p, 0);
    rb_define_method(rb_cNumeric, "nonzero?", num_nonzero_p, 0);

    rb_define_method(rb_cNumeric, "floor", num_floor, 0);
    rb_define_method(rb_cNumeric, "ceil", num_ceil, 0);
    rb_define_method(rb_cNumeric, "round", num_round, -1);
    rb_define_method(rb_cNumeric, "truncate", num_truncate, 0);
    rb_define_method(rb_cNumeric, "step", num_step, -1);

    rb_define_method(rb_cNumeric, "numerator", num_numerator, 0);
    rb_define_method(rb_cNumeric, "denominator", num_denominator, 0);

    rb_cInteger = rb_define_class("Integer", rb_cNumeric);
    rb_undef_alloc_func(rb_cInteger);
    rb_undef_method(CLASS_OF(rb_cInteger), "new");

    rb_define_method(rb_cInteger, "integer?", int_int_p, 0);
    rb_define_method(rb_cInteger, "odd?", int_odd_p, 0);
    rb_define_method(rb_cInteger, "even?", int_even_p, 0);
    rb_define_method(rb_cInteger, "upto", int_upto, 1);
    rb_define_method(rb_cInteger, "downto", int_downto, 1);
    rb_define_method(rb_cInteger, "times", int_dotimes, 0);
    rb_include_module(rb_cInteger, rb_mPrecision);
    rb_define_method(rb_cInteger, "succ", int_succ, 0);
    rb_define_method(rb_cInteger, "next", int_succ, 0);
    rb_define_method(rb_cInteger, "pred", int_pred, 0);
    rb_define_method(rb_cInteger, "chr", int_chr, -1);
    rb_define_method(rb_cInteger, "to_i", int_to_i, 0);
    rb_define_method(rb_cInteger, "to_int", int_to_i, 0);
    rb_define_method(rb_cInteger, "floor", int_to_i, 0);
    rb_define_method(rb_cInteger, "ceil", int_to_i, 0);
    rb_define_method(rb_cInteger, "truncate", int_to_i, 0);
    rb_define_method(rb_cInteger, "round", int_round, -1);

    rb_cFixnum = rb_define_class("Fixnum", rb_cInteger);
    rb_include_module(rb_cFixnum, rb_mPrecision);
    rb_define_singleton_method(rb_cFixnum, "induced_from", rb_fix_induced_from, 1);
    rb_define_singleton_method(rb_cInteger, "induced_from", rb_int_induced_from, 1);

    rb_define_method(rb_cInteger, "numerator", int_numerator, 0);
    rb_define_method(rb_cInteger, "denominator", int_denominator, 0);

    rb_define_method(rb_cFixnum, "to_s", fix_to_s, -1);

    rb_define_method(rb_cFixnum, "-@", fix_uminus, 0);
    rb_define_method(rb_cFixnum, "+", fix_plus, 1);
    rb_define_method(rb_cFixnum, "-", fix_minus, 1);
    rb_define_method(rb_cFixnum, "*", fix_mul, 1);
    rb_define_method(rb_cFixnum, "/", fix_div, 1);
    rb_define_method(rb_cFixnum, "div", fix_idiv, 1);
    rb_define_method(rb_cFixnum, "%", fix_mod, 1);
    rb_define_method(rb_cFixnum, "modulo", fix_mod, 1);
    rb_define_method(rb_cFixnum, "divmod", fix_divmod, 1);
    rb_define_method(rb_cFixnum, "fdiv", fix_fdiv, 1);
    rb_define_method(rb_cFixnum, "**", fix_pow, 1);

    rb_define_method(rb_cFixnum, "abs", fix_abs, 0);

    rb_define_method(rb_cFixnum, "==", fix_equal, 1);
    rb_define_method(rb_cFixnum, "<=>", fix_cmp, 1);
    rb_define_method(rb_cFixnum, ">",  fix_gt, 1);
    rb_define_method(rb_cFixnum, ">=", fix_ge, 1);
    rb_define_method(rb_cFixnum, "<",  fix_lt, 1);
    rb_define_method(rb_cFixnum, "<=", fix_le, 1);

    rb_define_method(rb_cFixnum, "~", fix_rev, 0);
    rb_define_method(rb_cFixnum, "&", fix_and, 1);
    rb_define_method(rb_cFixnum, "|", fix_or,  1);
    rb_define_method(rb_cFixnum, "^", fix_xor, 1);
    rb_define_method(rb_cFixnum, "[]", fix_aref, 1);

    rb_define_method(rb_cFixnum, "<<", rb_fix_lshift, 1);
    rb_define_method(rb_cFixnum, ">>", rb_fix_rshift, 1);

    rb_define_method(rb_cFixnum, "to_f", fix_to_f, 0);
    rb_define_method(rb_cFixnum, "size", fix_size, 0);
    rb_define_method(rb_cFixnum, "zero?", fix_zero_p, 0);
    rb_define_method(rb_cFixnum, "odd?", fix_odd_p, 0);
    rb_define_method(rb_cFixnum, "even?", fix_even_p, 0);
    rb_define_method(rb_cFixnum, "succ", fix_succ, 0);

    rb_cFloat  = rb_define_class("Float", rb_cNumeric);

    rb_undef_alloc_func(rb_cFloat);
    rb_undef_method(CLASS_OF(rb_cFloat), "new");

    rb_define_singleton_method(rb_cFloat, "induced_from", rb_flo_induced_from, 1);
    rb_include_module(rb_cFloat, rb_mPrecision);

    rb_define_const(rb_cFloat, "ROUNDS", INT2FIX(FLT_ROUNDS));
    rb_define_const(rb_cFloat, "RADIX", INT2FIX(FLT_RADIX));
    rb_define_const(rb_cFloat, "MANT_DIG", INT2FIX(DBL_MANT_DIG));
    rb_define_const(rb_cFloat, "DIG", INT2FIX(DBL_DIG));
    rb_define_const(rb_cFloat, "MIN_EXP", INT2FIX(DBL_MIN_EXP));
    rb_define_const(rb_cFloat, "MAX_EXP", INT2FIX(DBL_MAX_EXP));
    rb_define_const(rb_cFloat, "MIN_10_EXP", INT2FIX(DBL_MIN_10_EXP));
    rb_define_const(rb_cFloat, "MAX_10_EXP", INT2FIX(DBL_MAX_10_EXP));
    rb_define_const(rb_cFloat, "MIN", DOUBLE2NUM(DBL_MIN));
    rb_define_const(rb_cFloat, "MAX", DOUBLE2NUM(DBL_MAX));
    rb_define_const(rb_cFloat, "EPSILON", DOUBLE2NUM(DBL_EPSILON));

    rb_define_method(rb_cFloat, "to_s", flo_to_s, 0);
    rb_define_method(rb_cFloat, "coerce", flo_coerce, 1);
    rb_define_method(rb_cFloat, "-@", flo_uminus, 0);
    rb_define_method(rb_cFloat, "+", flo_plus, 1);
    rb_define_method(rb_cFloat, "-", flo_minus, 1);
    rb_define_method(rb_cFloat, "*", flo_mul, 1);
    rb_define_method(rb_cFloat, "/", flo_div, 1);
    rb_define_method(rb_cFloat, "quo", flo_quo, 1);
    rb_define_method(rb_cFloat, "fdiv", flo_quo, 1);
    rb_define_method(rb_cFloat, "%", flo_mod, 1);
    rb_define_method(rb_cFloat, "modulo", flo_mod, 1);
    rb_define_method(rb_cFloat, "divmod", flo_divmod, 1);
    rb_define_method(rb_cFloat, "**", flo_pow, 1);
    rb_define_method(rb_cFloat, "==", flo_eq, 1);
    rb_define_method(rb_cFloat, "<=>", flo_cmp, 1);
    rb_define_method(rb_cFloat, ">",  flo_gt, 1);
    rb_define_method(rb_cFloat, ">=", flo_ge, 1);
    rb_define_method(rb_cFloat, "<",  flo_lt, 1);
    rb_define_method(rb_cFloat, "<=", flo_le, 1);
    rb_define_method(rb_cFloat, "eql?", flo_eql, 1);
    rb_define_method(rb_cFloat, "hash", flo_hash, 0);
    rb_define_method(rb_cFloat, "to_f", flo_to_f, 0);
    rb_define_method(rb_cFloat, "abs", flo_abs, 0);
    rb_define_method(rb_cFloat, "zero?", flo_zero_p, 0);

    rb_define_method(rb_cFloat, "to_i", flo_truncate, 0);
    rb_define_method(rb_cFloat, "to_int", flo_truncate, 0);
    rb_define_method(rb_cFloat, "floor", flo_floor, 0);
    rb_define_method(rb_cFloat, "ceil", flo_ceil, 0);
    rb_define_method(rb_cFloat, "round", flo_round, -1);
    rb_define_method(rb_cFloat, "truncate", flo_truncate, 0);

    rb_define_method(rb_cFloat, "nan?",      flo_is_nan_p, 0);
    rb_define_method(rb_cFloat, "infinite?", flo_is_infinite_p, 0);
    rb_define_method(rb_cFloat, "finite?",   flo_is_finite_p, 0);

#if WITH_OBJC
    rb_install_nsnumber_primitives();
    rb_install_nsnumber_methods();
#endif
}