import pythonpath # adjusts the Python path
import knime_parameter as kp
import unittest

class ParameterizedObject:

    def __init__(self):
        self.default_x = 3

    @kp.Parameter
    def x(self):
        """The x parameter is the key to unlocking your true potential!"""
        return self.default_x

    @x.validator
    def x(self, value):
        if value < 0:
            raise ValueError("Negative values are not permitted!")


class ParameterTest(unittest.TestCase):
    def setUp(self) -> None:
        self.obj = ParameterizedObject()

    def test_get_default(self):
        self.assertEqual(self.obj.default_x, self.obj.x)

    def test_set_and_get(self) -> None:
        self.obj.x = 5
        self.assertEqual(5, self.obj.x)
        
    def test_set_invalid_value(self) -> None:
        self.obj.x = 5
        try:
            self.obj.x = -1
            self.fail("Negative values should cause an exception!")
        except ValueError:
            pass # expected behavior
        self.assertEqual(5, self.obj.x)